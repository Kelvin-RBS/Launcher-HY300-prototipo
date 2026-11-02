package br.hy300.games

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder
import java.security.MessageDigest
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

class GameInfo(val id: String, val title: String, val file: String, val system: String, val size: Long)

/** O que o servidor precisa saber fazer com os jogos (no app, quem responde é o AppGameStore). */
interface GameStore {
    fun list(): List<GameInfo>
    /** null = jogo não encontrado. */
    fun delete(id: String, withSaves: Boolean): Boolean?
    /** null = jogo não encontrado. */
    fun rename(id: String, newBase: String): GameFiles.Outcome?
    fun upload(fileName: String, input: InputStream, length: Long): GameFiles.Outcome
}

object NetInfo {
    /** Endereço IPv4 da rede local (Wi-Fi ou cabo), ou null se não houver. */
    fun localIpv4(): String? {
        var fallback: String? = null
        try {
            for (ni in NetworkInterface.getNetworkInterfaces().toList()) {
                if (!ni.isUp || ni.isLoopback) continue
                for (a in ni.inetAddresses.toList()) {
                    if (a is Inet4Address && !a.isLoopbackAddress) {
                        if (a.isSiteLocalAddress) return a.hostAddress
                        if (fallback == null) fallback = a.hostAddress
                    }
                }
            }
        } catch (e: Exception) {
            // sem rede
        }
        return fallback
    }
}

/**
 * Servidor web pequeno para a rede local: abre uma página em que o celular ou o computador envia,
 * renomeia e apaga jogos. Tudo exige o PIN mostrado na tela do projetor; depois de 5 erros seguidos
 * o servidor recusa tudo por 30 segundos. Só deve ficar ligado enquanto a tela "Gerenciar jogos" está aberta.
 */
class GameServer(
    private val store: GameStore,
    private val pin: String,
    private val page: () -> String,
    private val onChange: () -> Unit = {}
) {
    private var server: ServerSocket? = null
    private val pool = Executors.newFixedThreadPool(4)
    private val failures = AtomicInteger(0)
    @Volatile private var lockedUntil = 0L

    /** Liga o servidor na primeira porta livre a partir de [firstPort] e devolve a porta. */
    fun start(firstPort: Int = 8080): Int {
        var lastError: Exception? = null
        for (port in firstPort until firstPort + 10) {
            try {
                val s = ServerSocket()
                s.reuseAddress = true
                s.bind(InetSocketAddress(port))
                server = s
                val t = Thread({ acceptLoop(s) }, "GameServer")
                t.isDaemon = true
                t.start()
                return port
            } catch (e: Exception) {
                lastError = e
            }
        }
        throw lastError ?: IllegalStateException("sem porta livre")
    }

    fun stop() {
        try {
            server?.close()
        } catch (e: Exception) {
            // já fechado
        }
        server = null
        pool.shutdownNow()
    }

    private fun acceptLoop(s: ServerSocket) {
        while (!s.isClosed) {
            try {
                val c = s.accept()
                pool.execute { handle(c) }
            } catch (e: Exception) {
                if (s.isClosed) return
            }
        }
    }

    // ------------------------------------------------------------------ HTTP

    private class Bounded(private val src: InputStream, private var left: Long) : InputStream() {
        override fun read(): Int {
            if (left <= 0) return -1
            val b = src.read()
            if (b >= 0) left--
            return b
        }
        override fun read(b: ByteArray, off: Int, len: Int): Int {
            if (left <= 0) return -1
            val n = src.read(b, off, minOf(len.toLong(), left).toInt())
            if (n > 0) left -= n
            return n
        }
    }

    private fun readLine(inp: InputStream): String? {
        val sb = StringBuilder()
        while (true) {
            val b = inp.read()
            if (b < 0) return if (sb.isEmpty()) null else sb.toString()
            if (b == '\n'.code) return sb.toString().trimEnd('\r')
            if (sb.length > 8192) return null
            sb.append(b.toChar())
        }
    }

    private fun handle(c: Socket) {
        try {
            c.use {
                c.soTimeout = 30000
                val inp = BufferedInputStream(c.getInputStream())
                val out = BufferedOutputStream(c.getOutputStream())
                val requestLine = readLine(inp) ?: return
                val parts = requestLine.split(" ")
                if (parts.size < 3) {
                    json(out, 400, false, "Pedido inválido.")
                    return
                }
                val headers = HashMap<String, String>()
                var count = 0
                while (true) {
                    val line = readLine(inp) ?: return
                    if (line.isEmpty()) break
                    if (++count > 64) return
                    val i = line.indexOf(':')
                    if (i > 0) headers[line.substring(0, i).trim().lowercase()] = line.substring(i + 1).trim()
                }
                val target = parts[1]
                val q = target.indexOf('?')
                val path = if (q >= 0) target.substring(0, q) else target
                val query = parseQuery(if (q >= 0) target.substring(q + 1) else "")
                val length = headers["content-length"]?.toLongOrNull() ?: 0L
                route(parts[0], path, query, headers, length, inp, out)
                out.flush()
            }
        } catch (e: Exception) {
            // conexão caiu no meio: nada a fazer
        }
    }

    private fun parseQuery(raw: String): Map<String, String> {
        val map = HashMap<String, String>()
        if (raw.isEmpty()) return map
        for (pair in raw.split("&")) {
            val i = pair.indexOf('=')
            val k = if (i >= 0) pair.substring(0, i) else pair
            val v = if (i >= 0) pair.substring(i + 1) else ""
            try {
                map[URLDecoder.decode(k, "UTF-8")] = URLDecoder.decode(v, "UTF-8")
            } catch (e: Exception) {
                // parâmetro malformado: ignora
            }
        }
        return map
    }

    private fun authorized(headers: Map<String, String>): Int {
        val now = System.currentTimeMillis()
        if (now < lockedUntil) return 429
        val given = headers["x-pin"] ?: ""
        if (MessageDigest.isEqual(given.toByteArray(Charsets.UTF_8), pin.toByteArray(Charsets.UTF_8))) {
            failures.set(0)
            return 0
        }
        if (failures.incrementAndGet() >= 5) {
            failures.set(0)
            lockedUntil = now + 30_000
        }
        return 401
    }

    private fun route(
        method: String, path: String, query: Map<String, String>, headers: Map<String, String>,
        length: Long, inp: InputStream, out: OutputStream
    ) {
        if (method == "GET" && (path == "/" || path == "/index.html")) {
            bytes(out, 200, "text/html; charset=utf-8", page().toByteArray(Charsets.UTF_8))
            return
        }
        if (!path.startsWith("/api/")) {
            json(out, 404, false, "Não encontrado.")
            return
        }
        when (authorized(headers)) {
            401 -> { json(out, 401, false, "PIN incorreto."); return }
            429 -> { json(out, 429, false, "Muitas tentativas. Aguarde 30 segundos."); return }
        }
        if (headers["expect"]?.lowercase() == "100-continue") {
            out.write("HTTP/1.1 100 Continue\r\n\r\n".toByteArray())
            out.flush()
        }
        when {
            method == "GET" && path == "/api/list" -> {
                val sb = StringBuilder("{\"ok\":true,\"games\":[")
                store.list().forEachIndexed { i, g ->
                    if (i > 0) sb.append(',')
                    sb.append("{\"id\":").append(q(g.id)).append(",\"title\":").append(q(g.title))
                        .append(",\"file\":").append(q(g.file)).append(",\"system\":").append(q(g.system))
                        .append(",\"size\":").append(g.size).append('}')
                }
                sb.append("]}")
                bytes(out, 200, "application/json; charset=utf-8", sb.toString().toByteArray(Charsets.UTF_8))
            }
            method == "POST" && path == "/api/upload" -> {
                if (length > GameFiles.MAX_UPLOAD) {
                    json(out, 413, false, "Arquivo grande demais (o limite é 64 MB).")
                    return
                }
                val name = query["name"] ?: ""
                outcome(out, store.upload(name, Bounded(inp, length), length), name, false)
            }
            method == "POST" && path == "/api/delete" -> {
                val ok = store.delete(query["id"] ?: "", query["saves"] == "1")
                when (ok) {
                    null -> json(out, 404, false, "Jogo não encontrado.")
                    true -> { json(out, 200, true, null); onChange() }
                    false -> json(out, 500, false, "Não foi possível apagar o arquivo.")
                }
            }
            method == "POST" && path == "/api/rename" -> {
                val o = store.rename(query["id"] ?: "", query["name"] ?: "")
                if (o == null) json(out, 404, false, "Jogo não encontrado.") else outcome(out, o, o.file?.name ?: "", true)
            }
            else -> json(out, 404, false, "Não encontrado.")
        }
    }

    private fun outcome(out: OutputStream, o: GameFiles.Outcome, name: String, rename: Boolean) {
        when (o.result) {
            GameFiles.Result.OK -> {
                bytes(out, 200, "application/json; charset=utf-8",
                    ("{\"ok\":true,\"file\":" + q(o.file?.name ?: name) + "}").toByteArray(Charsets.UTF_8))
                onChange()
            }
            GameFiles.Result.INVALID -> json(out, 400, false,
                if (rename) "Nome inválido." else "Nome ou tipo de arquivo inválido (use .sfc, .smc, .swc, .fig, .nes ou .zip).")
            GameFiles.Result.EXISTS -> json(out, 409, false, "Já existe um jogo com esse nome.")
            GameFiles.Result.TOO_BIG -> json(out, 413, false, "Arquivo grande demais (o limite é 64 MB).")
            GameFiles.Result.BAD_ZIP -> json(out, 400, false, "O .zip não tem uma ROM de SNES ou NES.")
            GameFiles.Result.FAILED -> json(out, 500, false, "Não foi possível salvar o arquivo.")
        }
    }

    private fun q(s: String): String {
        val sb = StringBuilder("\"")
        for (c in s) {
            when {
                c == '"' -> sb.append("\\\"")
                c == '\\' -> sb.append("\\\\")
                c.code < 0x20 -> sb.append(String.format("\\u%04x", c.code))
                else -> sb.append(c)
            }
        }
        return sb.append('"').toString()
    }

    private fun json(out: OutputStream, status: Int, ok: Boolean, error: String?) {
        val body = if (ok) "{\"ok\":true}" else "{\"ok\":false,\"error\":" + q(error ?: "Erro.") + "}"
        bytes(out, status, "application/json; charset=utf-8", body.toByteArray(Charsets.UTF_8))
    }

    private fun bytes(out: OutputStream, status: Int, type: String, body: ByteArray) {
        val text = when (status) {
            200 -> "OK"; 400 -> "Bad Request"; 401 -> "Unauthorized"; 404 -> "Not Found"
            409 -> "Conflict"; 413 -> "Payload Too Large"; 429 -> "Too Many Requests"; else -> "Error"
        }
        val head = "HTTP/1.1 $status $text\r\nContent-Type: $type\r\nContent-Length: ${body.size}\r\n" +
            "Cache-Control: no-store\r\nConnection: close\r\n\r\n"
        out.write(head.toByteArray(Charsets.ISO_8859_1))
        out.write(body)
        out.flush()
    }
}

package br.hy300.tv

import android.content.Context
import android.os.Environment
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** Um canal da lista .m3u, já com o número usado no controle remoto. */
data class Channel(
    val number: Int,
    val name: String,
    val url: String,
    val group: String,
    val logo: String?,
    /** Nome do guia (tvg-name): costuma trazer a qualidade ("Globo HD"). */
    val guideName: String = ""
)

/** Lê listas no formato M3U/M3U8 (o mesmo que praticamente todo provedor entrega). */
object Playlist {

    fun parse(text: String): List<Channel> {
        val out = ArrayList<Channel>()
        var extinf: String? = null
        var n = 0
        for (raw in text.lineSequence()) {
            val line = raw.trim().trimStart('\uFEFF')
            if (line.isEmpty()) continue
            if (line.startsWith("#EXTINF", ignoreCase = true)) {
                extinf = line
                continue
            }
            if (line.startsWith("#")) continue // #EXTM3U, #EXTVLCOPT, #EXTGRP...
            val info = extinf
            extinf = null
            n++
            out.add(
                Channel(
                    number = n,
                    name = nameOf(info) ?: "Canal $n",
                    url = line,
                    group = (attr(info, "group-title") ?: "Sem categoria")
                        .substringBefore(';').trim().ifBlank { "Sem categoria" },
                    logo = attr(info, "tvg-logo"),
                    guideName = attr(info, "tvg-name") ?: ""
                )
            )
        }
        return out
    }

    /** Nome do canal: o texto depois da última vírgula que está fora das aspas. */
    private fun nameOf(extinf: String?): String? {
        if (extinf == null) return null
        var inQuotes = false
        var lastComma = -1
        for (i in extinf.indices) {
            when (extinf[i]) {
                '"' -> inQuotes = !inQuotes
                ',' -> if (!inQuotes) lastComma = i
            }
        }
        if (lastComma < 0) return null
        return extinf.substring(lastComma + 1).trim().ifBlank { null }
    }

    private fun attr(extinf: String?, key: String): String? {
        if (extinf == null) return null
        val m = Regex("(?i)" + Regex.escape(key) + "\\s*=\\s*\"([^\"]*)\"").find(extinf)
        return m?.groupValues?.get(1)?.trim()?.ifBlank { null }
    }
}

/** Lista carregada em memória, compartilhada entre as telas. */
object Library {
    @Volatile var channels: List<Channel> = emptyList()
    @Volatile var origin: String? = null
    @Volatile var error: String? = null
    @Volatile var loading = false

    /** Quantos canais a lista tinha antes do filtro de qualidade. */
    @Volatile var totalFound = 0

    /** Quantos canais pesados (4K/8K) foram deixados de fora. */
    @Volatile var heavyDropped = 0

    /** Aviso curto para a tela (ex.: lista sem canal nenhum depois do filtro). */
    @Volatile var note: String? = null

    /** Marcado quando a fonte ou o filtro muda; a tela inicial recarrega ao voltar. */
    @Volatile var dirty = false

    /** Sobe a cada recarga bem-sucedida: a tela inicial sabe que precisa se redesenhar. */
    @Volatile var version = 0

    fun groups(): List<String> =
        channels.map { it.group }.distinct().sortedBy { it.lowercase() }

    fun firstOf(group: String?): Int =
        if (group == null) 0 else channels.indexOfFirst { it.group == group }.coerceAtLeast(0)

    /** Recarrega do arquivo/URL configurado. Chamar fora da thread de interface. */
    fun reload(ctx: Context) {
        loading = true
        error = null
        try {
            val text = when (Source.mode(ctx)) {
                Source.MODE_URL -> fetch(Source.m3uUrl(ctx))
                Source.MODE_XTREAM -> fetch(Source.xtreamUrl(ctx))
                else -> readLocal(ctx)
            }
            val parsed = Playlist.parse(text)
            if (parsed.isEmpty()) {
                error = "A lista abriu, mas não tem nenhum canal."
            } else {
                totalFound = parsed.size
                note = null
                val clean = Source.cleanHeavy(ctx)
                val kept = if (clean) parsed.filter { !Quality.isTooHeavy(it) } else parsed
                val use = if (clean && kept.isEmpty()) {
                    note = "Todos os canais são 4K/8K: mostrando a lista inteira."
                    parsed
                } else {
                    kept
                }
                heavyDropped = parsed.size - use.size
                // Renumera para o controle remoto seguir 1, 2, 3... mesmo com filtro ligado.
                channels = use.mapIndexed { i, c -> c.copy(number = i + 1) }
                origin = when (Source.mode(ctx)) {
                    Source.MODE_URL -> Source.url(ctx)
                    Source.MODE_XTREAM -> Source.host(ctx)
                    else -> readLocalName(ctx)
                }
                error = null
                dirty = false
                version++
            }
        } catch (e: Exception) {
            error = friendly(e)
        } finally {
            loading = false
        }
    }

    private fun friendly(e: Exception): String {
        val msg = e.message ?: e.javaClass.simpleName
        return msg.take(180)
    }

    // ------------------------------------------------------------ arquivo local

    private fun downloadDir(): File =
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

    private fun candidates(ctx: Context): List<File> {
        val list = ArrayList<File>()
        val appDir = ctx.getExternalFilesDir(null)
        if (appDir != null) {
            list.add(File(appDir, "lista.m3u"))
            list.add(File(appDir, "lista.m3u8"))
        }
        val dl = downloadDir()
        list.add(File(dl, "tvaovivo.m3u"))
        list.add(File(dl, "tvaovivo.m3u8"))
        if (dl.isDirectory) {
            val found = dl.listFiles { f ->
                f.isFile && (f.name.endsWith(".m3u", true) || f.name.endsWith(".m3u8", true))
            }?.sortedBy { it.name.lowercase() }
            if (found != null) list.addAll(found)
        }
        return list
    }

    private fun localFile(ctx: Context): File? =
        candidates(ctx).firstOrNull { it.isFile && it.canRead() && it.length() > 0 }

    private fun readLocalName(ctx: Context): String? = localFile(ctx)?.name

    private fun readLocal(ctx: Context): String {
        val f = localFile(ctx)
            ?: throw IllegalStateException(
                "Nenhuma lista .m3u encontrada. Coloque o arquivo em Download/tvaovivo.m3u " +
                    "ou configure um endereço."
            )
        return decode(f.readBytes())
    }

    // ------------------------------------------------------------ rede

    internal fun fetch(address: String): String {
        val conn = URL(address).openConnection() as HttpURLConnection
        return try {
            conn.connectTimeout = 15000
            conn.readTimeout = 25000
            conn.instanceFollowRedirects = true
            conn.setRequestProperty("User-Agent", "TV-AO-VIVO/0.1")
            conn.setRequestProperty("Accept-Encoding", "identity")
            val code = conn.responseCode
            if (code !in 200..299) throw IllegalStateException("O servidor respondeu $code.")
            decode(conn.inputStream.use { it.readBytes() })
        } finally {
            conn.disconnect()
        }
    }

    /** UTF-8 na maioria dos casos; cai para Latin-1 quando o texto vem cheio de "?" de conversão. */
    private fun decode(bytes: ByteArray): String {
        val utf8 = String(bytes, Charsets.UTF_8)
        val bad = utf8.count { it == '\uFFFD' }
        return if (bad > 8) String(bytes, Charsets.ISO_8859_1) else utf8
    }
}

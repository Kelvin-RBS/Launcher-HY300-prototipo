package br.hy300.games

import android.content.Context
import android.util.Log
import br.hy300.games.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

/**
 * Catálogo de jogos do GitHub (bsnesemulator/SNES-ROMS).
 * Busca a lista da API do GitHub (paginada), faz cache local e só re-baixa se houver novidade.
 */
object GitHubCatalog {
    private const val TAG = "GitHubCatalog"

    /** Repositório dos jogos */
    private const val REPO_OWNER = "bsnesemulator"
    private const val REPO_NAME = "SNES-ROMS"
    private const val REPO_BRANCH = "main"

    private const val API_REPO = "https://api.github.com/repos/$REPO_OWNER/$REPO_NAME"

    /** Arquivo de cache local (v2: o cache antigo podia estar vazio por causa do bug do filtro) */
    private const val CACHE_FILE = "bsnes_catalog_v2.json"
    private const val CACHE_META_FILE = "bsnes_catalog_meta_v2.json"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
            val token = BuildConfig.GITHUB_TOKEN
            if (token.isNotBlank()) {
                request.addHeader("Authorization", "Bearer $token")
            }
            chain.proceed(request.build())
        }
        .build()

    data class GameInfo(
        val id: String,
        val title: String,
        val system: String,        // "snes" ou "nes"
        val fileName: String,      // nome do arquivo (ex: "Super Mario World.sfc")
        val size: Long,            // tamanho em bytes
        val sha256: String,        // hash (vazio = não verificado)
        val version: String,       // commit SHA ou "main"
        val downloadUrl: String,   // URL completa do raw.githubusercontent.com
        val description: String? = null
    )

    data class GameCatalog(
        val version: String,       // commit SHA do repo
        val updatedAt: String,     // timestamp da busca
        val games: List<GameInfo>
    )

    data class CacheMeta(
        val commitSha: String,
        val fetchedAt: Long
    )

    /** Extensões aceitas. No repo, quase tudo é .zip com a ROM de SNES dentro. */
    private val SNES_EXT = setOf("sfc", "smc", "swc", "fig", "zip")
    private val NES_EXT = setOf("nes")

    /**
     * Busca o catálogo: usa cache se válido, senão baixa da API do GitHub.
     * Se a rede/API falhar (ex.: limite de 60 requisições/h sem token) e existir cache antigo, usa o cache.
     * @param forceUpdate Se true, ignora cache e busca novamente
     */
    fun fetchCatalog(ctx: Context, forceUpdate: Boolean = false): GameCatalog? {
        val cached = readCache(ctx)
        if (!forceUpdate && cached != null) {
            Log.d(TAG, "Usando cache local (${cached.games.size} jogos)")
            return cached
        }
        Log.d(TAG, "Buscando catálogo da API do GitHub...")
        return fetchFromGitHub(ctx) ?: cached
    }

    /**
     * Verifica se há atualização no repositório: compara o SHA do último commit com o do cache.
     * (O ETag NÃO é o SHA do commit, então a comparação antiga sempre dava "mudou".)
     */
    fun checkForUpdate(ctx: Context): Boolean {
        val meta = readCacheMeta(ctx) ?: return true // sem cache = precisa buscar
        val sha = fetchCommitSha() ?: return false   // sem rede/limite: mantém o que já tem
        return sha != meta.commitSha
    }

    /**
     * Lista o repositório INTEIRO com uma única chamada (git/trees?recursive=1).
     * A API "contents" antiga cortava em 1000 arquivos e ignorava a paginação (repetia a mesma página).
     */
    private fun fetchFromGitHub(ctx: Context): GameCatalog? {
        val commitSha = fetchCommitSha() ?: return null

        val request = Request.Builder()
            .url("$API_REPO/git/trees/$commitSha?recursive=1")
            .addHeader("Accept", "application/vnd.github+json")
            .build()

        val tree = executeWithRetry(request, maxAttempts = 3) { response ->
            if (!response.isSuccessful) {
                Log.w(TAG, "Falha ao listar árvore: HTTP ${response.code}")
                return@executeWithRetry null
            }
            val body = response.body?.string() ?: return@executeWithRetry null
            JSONObject(body)
        } ?: return null

        if (tree.optBoolean("truncated", false)) {
            Log.w(TAG, "Árvore do repo truncada pela API (repo muito grande)")
        }

        val items = tree.optJSONArray("tree") ?: return null
        val games = ArrayList<GameInfo>()
        for (i in 0 until items.length()) {
            val item = items.getJSONObject(i)
            if (item.optString("type") != "blob") continue
            parseGameFile(item.getString("path"), item.optLong("size", 0L))?.let { games.add(it) }
        }
        games.sortBy { it.title.lowercase() }

        Log.d(TAG, "Jogos válidos encontrados: ${games.size} de ${items.length()} itens")
        if (games.isEmpty()) return null // nunca grava cache vazio

        val catalog = GameCatalog(
            version = commitSha,
            updatedAt = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US)
                .format(java.util.Date()),
            games = games
        )
        writeCache(ctx, catalog, commitSha)
        return catalog
    }

    /** SHA do último commit da branch (Accept sha devolve só o hash, em texto) */
    private fun fetchCommitSha(): String? {
        val request = Request.Builder()
            .url("$API_REPO/commits/$REPO_BRANCH")
            .addHeader("Accept", "application/vnd.github.sha")
            .build()

        return executeWithRetry(request, maxAttempts = 3) { response ->
            if (!response.isSuccessful) {
                Log.w(TAG, "Falha ao ler commit: HTTP ${response.code}")
                return@executeWithRetry null
            }
            response.body?.string()?.trim()?.takeIf { it.length >= 40 }
        }
    }

    /** Executa uma requisição com retry para rate limiting (HTTP 403/429) */
    private fun <T> executeWithRetry(request: Request, maxAttempts: Int = 3, attempt: Int = 1, block: (Response) -> T?): T? {
        return try {
            client.newCall(request).execute().use { response ->
                val limited = (response.code == 403 && response.header("X-RateLimit-Remaining") == "0") || response.code == 429
                if (limited) {
                    val reset = response.header("X-RateLimit-Reset")?.toLongOrNull() ?: 0L
                    val waitSeconds = if (reset > 0) Math.max(1L, reset - (System.currentTimeMillis() / 1000)) else attempt * 30L
                    // Esperar mais de 1 minuto trava a tela: desiste e deixa o app usar o cache
                    if (attempt < maxAttempts && waitSeconds <= 60L) {
                        Log.w(TAG, "Rate limited, aguardando ${waitSeconds}s (tentativa $attempt/$maxAttempts)")
                        Thread.sleep(waitSeconds * 1000)
                        return@use executeWithRetry(request, maxAttempts, attempt + 1, block)
                    }
                    Log.e(TAG, "Limite da API do GitHub atingido (renova em ${waitSeconds}s)")
                    return@use null
                }
                block(response)
            }
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            Log.e(TAG, "Interrompido", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "Erro na requisição", e)
            null
        }
    }

    /** Converte um arquivo da árvore (path relativo à raiz do repo) para GameInfo */
    private fun parseGameFile(path: String, size: Long): GameInfo? {
        val name = path.substringAfterLast('/')
        if (name.startsWith(".")) return null // .gitignore, .gitkeep...

        val ext = name.substringAfterLast('.', "").lowercase()
        val system = when (ext) {
            in SNES_EXT -> "snes"
            in NES_EXT -> "nes"
            else -> return null
        }

        // URL raw montada com HttpUrl: codifica espaços, &, ' e vírgulas de cada pasta/arquivo
        val urlBuilder = okhttp3.HttpUrl.Builder()
            .scheme("https")
            .host("raw.githubusercontent.com")
            .addPathSegment(REPO_OWNER)
            .addPathSegment(REPO_NAME)
            .addPathSegment(REPO_BRANCH)
        path.split('/').forEach { urlBuilder.addPathSegment(it) }

        return GameInfo(
            id = "$system:$path", // estável entre atualizações (o índice antigo mudava)
            title = cleanTitle(name.substringBeforeLast('.')),
            system = system,
            fileName = name,
            size = size,
            sha256 = "", // o repo externo não fornece
            version = "main",
            downloadUrl = urlBuilder.build().toString(),
            description = null
        )
    }

    /** Limpa título: remove (USA), [b1], etc. */
    private fun cleanTitle(raw: String): String {
        var s = raw.replace('_', ' ')
        // Remove região e tags entre parênteses/colchetes
        s = s.replace(Regex("\\([^)]*\\)"), "").replace(Regex("\\[[^\\]]*\\]"), "")
        // Remove tags de idioma
        s = s.replace(Regex("(?i)\\bPT-?BR\\b"), "")
        // Limpa espaços
        return s.replace(Regex("\\s+"), " ").trim()
    }

    /** Baixa um jogo de raw.githubusercontent.com */
    fun downloadGame(
        ctx: Context,
        game: GameInfo,
        progressCallback: (Float) -> Unit
    ): GameFiles.Outcome {
        val request = Request.Builder().url(game.downloadUrl).build()

        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "Download ${game.fileName}: HTTP ${response.code}")
                    return@use GameFiles.Outcome(GameFiles.Result.FAILED)
                }

                val body = response.body ?: return@use GameFiles.Outcome(GameFiles.Result.FAILED)
                // Se o servidor não informar o tamanho, usa o do catálogo
                val contentLength = body.contentLength().takeIf { it > 0 } ?: game.size
                if (contentLength > GameFiles.MAX_UPLOAD) {
                    return@use GameFiles.Outcome(GameFiles.Result.TOO_BIG)
                }

                // Pasta de destino: /Jogos/<sistema>; sem acesso a todos os arquivos, usa a pasta do app
                val systemDir = pickSystemDir(ctx, game.system)

                // Nome seguro (o do repo tem vírgulas, & e apóstrofos, que o Android aceita; barras não)
                val ext = game.fileName.substringAfterLast('.', "")
                val base = GameFiles.cleanBaseName(game.fileName.substringBeforeLast('.')) ?: return@use GameFiles.Outcome(GameFiles.Result.INVALID)
                val targetFile = File(systemDir, "$base.$ext")
                if (targetFile.exists()) {
                    return@use GameFiles.Outcome(GameFiles.Result.EXISTS, targetFile)
                }

                val tempFile = File(systemDir, ".download-${System.nanoTime()}.part")
                var downloaded = 0L
                val buffer = ByteArray(65536)
                try {
                    body.byteStream().use { input ->
                        FileOutputStream(tempFile).use { output ->
                            while (true) {
                                val read = input.read(buffer)
                                if (read == -1) break
                                output.write(buffer, 0, read)
                                downloaded += read
                                if (contentLength > 0) {
                                    progressCallback((downloaded.toFloat() / contentLength).coerceAtMost(1f))
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    tempFile.delete()
                    throw e
                }

                // O repo externo não fornece SHA-256: confere só o tamanho recebido
                if (tempFile.length() != downloaded || (game.size > 0 && downloaded != game.size)) {
                    tempFile.delete()
                    return@use GameFiles.Outcome(GameFiles.Result.FAILED)
                }

                if (!tempFile.renameTo(targetFile)) {
                    tempFile.delete()
                    return@use GameFiles.Outcome(GameFiles.Result.FAILED)
                }

                GameFiles.Outcome(GameFiles.Result.OK, targetFile)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao baixar jogo ${game.id}", e)
            GameFiles.Outcome(GameFiles.Result.FAILED)
        }
    }

    private fun pickSystemDir(ctx: Context, system: String): File {
        if (Catalog.canReadPublic()) {
            val pub = File(Catalog.PUBLIC_DIR, system)
            if (pub.isDirectory || pub.mkdirs()) return pub
        }
        return File(Catalog.appRoot(ctx), system).also { it.mkdirs() }
    }

    // ========== CACHE LOCAL ==========

    private fun getCacheDir(ctx: Context): File = ctx.filesDir

    private fun readCache(ctx: Context): GameCatalog? {
        val file = File(getCacheDir(ctx), CACHE_FILE)
        if (!file.exists()) return null

        return try {
            val json = file.readText()
            parseCatalog(json)?.takeIf { it.games.isNotEmpty() }
        } catch (e: Exception) {
            Log.w(TAG, "Erro ao ler cache", e)
            null
        }
    }

    private fun readCacheMeta(ctx: Context): CacheMeta? {
        val file = File(getCacheDir(ctx), CACHE_META_FILE)
        if (!file.exists()) return null

        return try {
            val json = file.readText()
            val obj = JSONObject(json)
            CacheMeta(
                commitSha = obj.getString("commitSha"),
                fetchedAt = obj.getLong("fetchedAt")
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun writeCache(ctx: Context, catalog: GameCatalog, commitSha: String) {
        try {
            val file = File(getCacheDir(ctx), CACHE_FILE)
            val json = toJson(catalog)
            file.writeText(json)

            val metaFile = File(getCacheDir(ctx), CACHE_META_FILE)
            val metaJson = JSONObject()
                .put("commitSha", commitSha)
                .put("fetchedAt", System.currentTimeMillis())
                .toString()
            metaFile.writeText(metaJson)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao escrever cache", e)
        }
    }

    private fun toJson(catalog: GameCatalog): String {
        val obj = JSONObject()
        obj.put("version", catalog.version)
        obj.put("updatedAt", catalog.updatedAt)
        val arr = JSONArray()
        for (g in catalog.games) {
            val go = JSONObject()
            go.put("id", g.id)
            go.put("title", g.title)
            go.put("system", g.system)
            go.put("fileName", g.fileName)
            go.put("size", g.size)
            go.put("sha256", g.sha256)
            go.put("version", g.version)
            go.put("downloadUrl", g.downloadUrl)
            go.put("description", g.description ?: JSONObject.NULL)
            arr.put(go)
        }
        obj.put("games", arr)
        return obj.toString()
    }

    private fun parseCatalog(jsonString: String): GameCatalog? {
        return try {
            val json = JSONObject(jsonString)
            val version = json.getString("version")
            val updatedAt = json.getString("updatedAt")
            val gamesArray = json.getJSONArray("games")
            val games = mutableListOf<GameInfo>()

            for (i in 0 until gamesArray.length()) {
                val g = gamesArray.getJSONObject(i)
                games.add(GameInfo(
                    id = g.getString("id"),
                    title = g.getString("title"),
                    system = g.getString("system"),
                    fileName = g.getString("fileName"),
                    size = g.getLong("size"),
                    sha256 = g.getString("sha256"),
                    version = g.getString("version"),
                    downloadUrl = g.getString("downloadUrl"),
                    description = g.optString("description").takeIf { it.isNotBlank() }
                ))
            }
            GameCatalog(version, updatedAt, games)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao parsear cache", e)
            null
        }
    }

    /** Formata tamanho */
    fun humanSize(bytes: Long): String {
        return if (bytes >= 1048576L) {
            String.format(java.util.Locale("pt", "BR"), "%.1f MB", bytes / 1048576.0)
        } else {
            Math.max(1L, bytes / 1024L).toString() + " KB"
        }
    }
}
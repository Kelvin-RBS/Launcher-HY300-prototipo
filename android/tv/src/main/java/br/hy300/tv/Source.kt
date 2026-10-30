package br.hy300.tv

import android.content.Context

/** De onde vem a lista de canais. Tudo fica em SharedPreferences, nada sai do aparelho. */
object Source {
    const val MODE_FILE = "file"
    const val MODE_URL = "url"
    const val MODE_XTREAM = "xtream"

    private const val PREFS = "fonte"
    private const val K_MODE = "mode"
    private const val K_URL = "url"
    private const val K_HOST = "host"
    private const val K_USER = "user"
    private const val K_PASS = "pass"
    private const val K_OUTPUT = "output" // hls ou ts
    private const val K_CLEAN = "remover_pesados"

    private fun p(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun mode(ctx: Context): String = p(ctx).getString(K_MODE, MODE_FILE) ?: MODE_FILE
    fun setMode(ctx: Context, v: String) = p(ctx).edit().putString(K_MODE, v).apply()

    /**
     * Remove canais 4K/8K (padrão). SD e HD ficam: o projetor entrega 720p, então
     * 4K só esquenta o decoder e trava.
     */
    fun cleanHeavy(ctx: Context): Boolean = p(ctx).getBoolean(K_CLEAN, true)
    fun setCleanHeavy(ctx: Context, v: Boolean) = p(ctx).edit().putBoolean(K_CLEAN, v).apply()

    fun url(ctx: Context): String = p(ctx).getString(K_URL, "") ?: ""
    fun setUrl(ctx: Context, v: String) = p(ctx).edit().putString(K_URL, v).apply()

    fun host(ctx: Context): String = p(ctx).getString(K_HOST, "") ?: ""
    fun user(ctx: Context): String = p(ctx).getString(K_USER, "") ?: ""
    fun pass(ctx: Context): String = p(ctx).getString(K_PASS, "") ?: ""
    fun output(ctx: Context): String = p(ctx).getString(K_OUTPUT, "ts") ?: "ts"

    fun setXtream(ctx: Context, host: String, user: String, pass: String, output: String) {
        p(ctx).edit()
            .putString(K_HOST, host)
            .putString(K_USER, user)
            .putString(K_PASS, pass)
            .putString(K_OUTPUT, output)
            .apply()
    }

    fun setOutput(ctx: Context, v: String) = p(ctx).edit().putString(K_OUTPUT, v).apply()

    /** URL .m3u digitada à mão. */
    fun m3uUrl(ctx: Context): String = url(ctx)

    /** Monta a URL de lista do Xtream Codes (não precisa de API JSON para tocar TV). */
    fun xtreamUrl(ctx: Context): String {
        val host = host(ctx).trim().trimEnd('/')
        if (host.isEmpty()) throw IllegalStateException("Falta o endereço do servidor Xtream.")
        val base = if (host.startsWith("http://") || host.startsWith("https://")) host else "http://$host"
        return "$base/get.php?username=${user(ctx)}&password=${pass(ctx)}" +
            "&type=m3u_plus&output=${output(ctx)}"
    }

    /** Resumo curto para mostrar na tela. */
    fun describe(ctx: Context): String = when (mode(ctx)) {
        MODE_URL -> if (url(ctx).isBlank()) "Endereço não configurado" else url(ctx)
        MODE_XTREAM -> if (host(ctx).isBlank()) "Xtream não configurado" else "${host(ctx)}  ·  ${user(ctx)}"
        else -> "Arquivo .m3u em Download/tvaovivo.m3u"
    }
}

/** Canais favoritos (guardados por nome + endereço do stream). */
object Favorites {
    private const val PREFS = "favoritos"
    private const val KEY = "itens"

    private fun key(c: Channel) = c.name + "|" + c.url

    fun load(ctx: Context): MutableSet<String> {
        val set = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getStringSet(KEY, emptySet()) ?: emptySet()
        return LinkedHashSet(set)
    }

    fun has(ctx: Context, c: Channel): Boolean = load(ctx).contains(key(c))

    /** Devolve o novo estado. */
    fun toggle(ctx: Context, c: Channel): Boolean {
        val set = load(ctx)
        val k = key(c)
        val added = if (set.contains(k)) {
            set.remove(k); false
        } else {
            set.add(k); true
        }
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putStringSet(KEY, set).apply()
        return added
    }

    fun isFavorite(set: Set<String>, c: Channel): Boolean = set.contains(key(c))
}

/** Lembra o último canal aberto, para retomar de onde parou. */
object LastChannel {
    private const val PREFS = "sessao"
    private const val KEY = "ultimo"

    fun get(ctx: Context): String? =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)

    fun set(ctx: Context, c: Channel) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, c.name + "|" + c.url).apply()
    }
}

package br.hy300.launcher

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable

/** Um app instalado que pode ser aberto pelo launcher. */
class AppEntry(
    val pkg: String,
    val label: String,
    val icon: Drawable,
    val banner: Drawable?
)

object AppRepo {
    /** true: usa o banner do app (formato de TV) quando existir; false: sempre o ícone. */
    const val PREFER_BANNER = true
    const val MAX_PINNED = 7

    private const val PREFS = "launcher"
    private const val KEY_PINNED = "pinned"

    /** Atalhos iniciais (só aparecem os que estiverem instalados). */
    val DEFAULT_PINNED = listOf(
        "com.netflix.mediaclient",
        "com.google.android.youtube.tv",
        "com.global.unitviptv",
        "tv.pluto.android",
        "com.mercadolibre.android.mplay_tv",
        "cm.aptoidetv.pt",
        "com.instantbits.cast.receiver"
    )

    fun launchIntent(pm: PackageManager, pkg: String): Intent? =
        pm.getLeanbackLaunchIntentForPackage(pkg) ?: pm.getLaunchIntentForPackage(pkg)

    /** Apps de TV (LEANBACK_LAUNCHER) e comuns (LAUNCHER), sem repetir, em ordem alfabética. */
    fun installed(ctx: Context): List<AppEntry> {
        val pm = ctx.packageManager
        val result = LinkedHashMap<String, AppEntry>()
        for (cat in listOf(Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER)) {
            val query = Intent(Intent.ACTION_MAIN).addCategory(cat)
            for (ri in pm.queryIntentActivities(query, 0)) {
                val ai = ri.activityInfo ?: continue
                val pkg = ai.packageName
                if (pkg == ctx.packageName || result.containsKey(pkg)) continue
                val banner = ai.loadBanner(pm) ?: ai.applicationInfo.loadBanner(pm)
                result[pkg] = AppEntry(pkg, ri.loadLabel(pm).toString(), ri.loadIcon(pm), banner)
            }
        }
        return result.values.sortedBy { it.label.lowercase() }
    }

    fun pinned(ctx: Context): List<String> {
        val raw = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_PINNED, null)
        return if (raw == null) DEFAULT_PINNED else raw.split(",").filter { it.isNotBlank() }
    }

    fun savePinned(ctx: Context, list: List<String>) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_PINNED, list.joinToString(",")).apply()
    }
}

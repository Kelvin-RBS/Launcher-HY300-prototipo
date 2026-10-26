package br.hy300.games

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

/** Motor de emulação da fase 1: o Snes9x EX+ já instalado no projetor. */
object Emulator {
    const val PACKAGE = "com.explusalpha.Snes9xPlus"
    private const val ACTIVITY = "com.imagine.BaseActivity"

    fun installed(pm: PackageManager): Boolean =
        try {
            pm.getPackageInfo(PACKAGE, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }

    fun versionName(pm: PackageManager): String? =
        try {
            pm.getPackageInfo(PACKAGE, 0).versionName
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }

    /** Abre o jogo no emulador. Devolve false se não foi possível. */
    fun launch(ctx: Context, game: Game): Boolean {
        val intent = Intent(Intent.ACTION_VIEW)
            .setComponent(ComponentName(PACKAGE, ACTIVITY))
            .setData(RomProvider.uriFor(game.source, game.rel))
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            ctx.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun openEmulatorMenu(ctx: Context): Boolean {
        val pm = ctx.packageManager
        val intent = pm.getLeanbackLaunchIntentForPackage(PACKAGE) ?: pm.getLaunchIntentForPackage(PACKAGE) ?: return false
        return try {
            ctx.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }
}

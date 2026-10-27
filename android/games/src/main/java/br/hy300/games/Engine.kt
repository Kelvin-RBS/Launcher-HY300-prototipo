package br.hy300.games

import android.content.Context
import java.io.File

/** Decide se o motor próprio pode ser usado (parte nativa compilada + núcleo do Snes9x presente). */
object Engine {
    private val CORES = listOf("libsnes9x2010_libretro_android.so", "libsnes9x_libretro_android.so")

    fun corePath(ctx: Context): String? {
        val dir = ctx.applicationInfo.nativeLibraryDir ?: return null
        for (name in CORES) {
            val f = File(dir, name)
            if (f.isFile) return f.absolutePath
        }
        return null
    }

    fun coreName(ctx: Context): String? =
        corePath(ctx)?.let { File(it).name.removePrefix("lib").removeSuffix("_libretro_android.so") }

    fun ownAvailable(ctx: Context): Boolean = NativeEmu.ready && corePath(ctx) != null
}

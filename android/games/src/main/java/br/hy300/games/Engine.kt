package br.hy300.games

import android.content.Context
import java.io.File

/** Decide se o motor próprio pode ser usado (parte nativa compilada + núcleo do sistema presente). */
object Engine {
    fun corePath(ctx: Context, system: String = "snes"): String? {
        val dir = ctx.applicationInfo.nativeLibraryDir ?: return null
        for (name in Systems.cores(system)) {
            val f = File(dir, name)
            if (f.isFile) return f.absolutePath
        }
        return null
    }

    fun coreName(ctx: Context, system: String = "snes"): String? =
        corePath(ctx, system)?.let { File(it).name.removePrefix("lib").removeSuffix("_libretro_android.so") }

    fun ownAvailable(ctx: Context, system: String = "snes"): Boolean =
        NativeEmu.ready && corePath(ctx, system) != null
}

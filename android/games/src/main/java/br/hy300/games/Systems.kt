package br.hy300.games

/** Sistemas de jogo suportados e os núcleos libretro (em ordem de preferência) de cada um. */
object Systems {
    fun label(system: String): String = if (system == "nes") "NES" else "Super Nintendo"

    fun cores(system: String): List<String> =
        if (system == "nes") {
            listOf("libfceumm_libretro_android.so", "libnestopia_libretro_android.so", "libquicknes_libretro_android.so")
        } else {
            listOf("libsnes9x2010_libretro_android.so", "libsnes9x_libretro_android.so")
        }
}

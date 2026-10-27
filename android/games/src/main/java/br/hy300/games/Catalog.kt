package br.hy300.games

import android.content.Context
import android.os.Environment
import java.io.File

/**
 * Um jogo. [source] diz de onde vem ("pub" = pasta pública /Jogos, "app" = pasta do próprio app)
 * e [rel] é o caminho relativo dentro dela.
 */
class Game(val system: String, val systemLabel: String, val source: String, val rel: String, val file: File) {
    val title: String = Catalog.cleanTitle(file.nameWithoutExtension)
}

object Catalog {
    /** Pasta pública dos jogos: o adb consegue enviar arquivos para ela. */
    const val PUBLIC_DIR = "/storage/emulated/0/Jogos"

    private val EXTENSIONS = listOf("sfc", "smc", "swc", "fig", "zip")

    /** Nome para exibir: tira o que está entre () e [] e a etiqueta de idioma "PT-BR". */
    fun cleanTitle(raw: String): String {
        val sb = StringBuilder()
        var depth = 0
        for (c in raw.replace('_', ' ')) {
            if (c == '(' || c == '[') {
                depth++
            } else if ((c == ')' || c == ']') && depth > 0) {
                depth--
            } else if (depth == 0) {
                sb.append(c)
            }
        }
        val tags = listOf("PT-BR", "PTBR")
        return sb.toString().split(" ").filter { it.isNotEmpty() && !tags.contains(it.uppercase()) }.joinToString(" ")
    }

    fun appRoot(ctx: Context): File = ctx.getExternalFilesDir("jogos") ?: File(ctx.filesDir, "jogos")

    fun baseFor(ctx: Context, source: String): File =
        if (source == "pub") File(PUBLIC_DIR) else appRoot(ctx)

    /** Ler a pasta pública exige o acesso a todos os arquivos (liberado por adb, veja Configurações). */
    fun canReadPublic(): Boolean = Environment.isExternalStorageManager()

    fun games(ctx: Context): List<Game> {
        val result = ArrayList<Game>()
        if (canReadPublic()) {
            scan(File(PUBLIC_DIR), "pub", "", result)
            scan(File(PUBLIC_DIR, "snes"), "pub", "snes/", result)
        }
        val appSnes = File(appRoot(ctx), "snes")
        appSnes.mkdirs()
        scan(appSnes, "app", "snes/", result)
        return result.sortedBy { it.title.lowercase() }
    }

    private fun scan(dir: File, source: String, prefix: String, out: MutableList<Game>) {
        val files = dir.listFiles() ?: return
        for (f in files) {
            val ext = f.extension.lowercase()
            if (f.isFile && EXTENSIONS.contains(ext) && (ext != "zip" || RomLoader.zipHasSnesRom(f))) {
                out.add(Game("snes", "Super Nintendo", source, prefix + f.name, f))
            }
        }
    }
}

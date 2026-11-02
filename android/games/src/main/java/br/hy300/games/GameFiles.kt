package br.hy300.games

import java.io.File
import java.io.IOException
import java.io.InputStream

/**
 * Operações em arquivos de jogo: renomear, apagar e receber um envio. Kotlin puro (sem Android),
 * por isso dá para testar no computador.
 */
object GameFiles {
    val UPLOAD_EXT = setOf("sfc", "smc", "swc", "fig", "nes", "zip")
    private val SNES_EXT = setOf("sfc", "smc", "swc", "fig")
    const val MAX_UPLOAD = 64L * 1024 * 1024

    enum class Result { OK, INVALID, EXISTS, TOO_BIG, BAD_ZIP, FAILED }
    class Outcome(val result: Result, val file: File? = null)

    /** Nome seguro (sem a extensão): tira o que o sistema de arquivos não aceita. Null se não sobrar nada. */
    fun cleanBaseName(raw: String): String? {
        val sb = StringBuilder()
        for (c in raw) {
            if (c.isISOControl() || "/\\:*?\"<>|".indexOf(c) >= 0) continue
            sb.append(c)
        }
        var s = sb.toString().trim().trim('.').trim()
        if (s.length > 100) s = s.substring(0, 100).trim()
        return if (s.isEmpty()) null else s
    }

    /** Renomeia o jogo mantendo a extensão e leva junto o save e o estado salvo. */
    fun rename(file: File, newBase: String, savesDir: File?, statesDir: File?): Outcome {
        val base = cleanBaseName(newBase) ?: return Outcome(Result.INVALID)
        val ext = file.extension
        val target = File(file.parentFile, if (ext.isEmpty()) base else base + "." + ext)
        if (target.name == file.name) return Outcome(Result.OK, file)
        // A pasta pública não diferencia maiúsculas: mudar só a caixa do nome é permitido
        if (target.exists() && !target.name.equals(file.name, ignoreCase = true)) return Outcome(Result.EXISTS)
        val oldName = file.nameWithoutExtension
        if (!file.renameTo(target)) return Outcome(Result.FAILED)
        moveSave(savesDir, oldName, target.nameWithoutExtension, ".srm")
        moveSave(statesDir, oldName, target.nameWithoutExtension, ".state")
        return Outcome(Result.OK, target)
    }

    private fun moveSave(dir: File?, oldName: String, newName: String, suffix: String) {
        if (dir == null) return
        val from = File(dir, oldName + suffix)
        val to = File(dir, newName + suffix)
        if (from.isFile && !to.exists()) from.renameTo(to)
    }

    /** Apaga o arquivo do jogo por completo e, se pedido, também o save e o estado salvo. */
    fun delete(file: File, alsoSaves: Boolean, savesDir: File?, statesDir: File?): Boolean {
        val name = file.nameWithoutExtension
        val ok = !file.exists() || file.delete()
        if (ok && alsoSaves) {
            if (savesDir != null) File(savesDir, name + ".srm").delete()
            if (statesDir != null) File(statesDir, name + ".state").delete()
        }
        return ok
    }

    /**
     * Grava um jogo recebido na pasta do sistema certo dentro de [root] (snes ou nes). Escreve num
     * arquivo temporário e só então dá o nome final, então um envio interrompido não deixa jogo pela metade.
     */
    fun saveUpload(root: File, fileName: String, input: InputStream, length: Long): Outcome {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        val base = cleanBaseName(fileName.substringBeforeLast('.', fileName))
        if (base == null || ext !in UPLOAD_EXT) return Outcome(Result.INVALID)
        if (length <= 0L) return Outcome(Result.INVALID)
        if (length > MAX_UPLOAD) return Outcome(Result.TOO_BIG)

        root.mkdirs()
        val temp = File(root, ".envio-" + System.nanoTime() + ".part")
        try {
            temp.outputStream().use { out ->
                val buf = ByteArray(65536)
                var left = length
                while (left > 0) {
                    val n = input.read(buf, 0, minOf(buf.size.toLong(), left).toInt())
                    if (n < 0) throw IOException("conexão encerrada antes do fim")
                    out.write(buf, 0, n)
                    left -= n
                }
            }
            val system = when {
                ext in SNES_EXT -> "snes"
                ext == "nes" -> "nes"
                else -> RomLoader.zipSystem(temp) ?: run { temp.delete(); return Outcome(Result.BAD_ZIP) }
            }
            val dir = File(root, system)
            dir.mkdirs()
            val target = File(dir, base + "." + ext)
            if (target.exists()) {
                temp.delete()
                return Outcome(Result.EXISTS)
            }
            if (!temp.renameTo(target)) {
                temp.delete()
                return Outcome(Result.FAILED)
            }
            return Outcome(Result.OK, target)
        } catch (e: Exception) {
            temp.delete()
            return Outcome(Result.FAILED)
        }
    }
}

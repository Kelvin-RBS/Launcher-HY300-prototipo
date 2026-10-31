package br.hy300.games

import java.io.File
import java.util.zip.ZipFile

object RomLoader {
    private val SNES_EXT = listOf("sfc", "smc", "swc", "fig")

    /** "snes", "nes" ou null, conforme a extensão do nome. */
    private fun systemOfName(name: String): String? {
        val ext = name.substringAfterLast('.', "").lowercase()
        return when {
            SNES_EXT.contains(ext) -> "snes"
            ext == "nes" -> "nes"
            else -> null
        }
    }

    /** Sistema da primeira ROM de dentro do .zip, ou null se não houver ROM suportada. */
    fun zipSystem(file: File): String? =
        try {
            ZipFile(file).use { zip ->
                zip.entries().asSequence().filter { !it.isDirectory }.mapNotNull { systemOfName(it.name) }.firstOrNull()
            }
        } catch (e: Exception) {
            null
        }

    /** Devolve (nome, bytes) da ROM. Se for .zip, extrai a primeira ROM de dentro. */
    fun readRom(file: File): Pair<String, ByteArray>? =
        try {
            if (file.extension.lowercase() == "zip") {
                ZipFile(file).use { zip ->
                    val entry = zip.entries().asSequence().firstOrNull { !it.isDirectory && systemOfName(it.name) != null }
                    if (entry == null) null else zip.getInputStream(entry).use { Pair(entry.name, it.readBytes()) }
                }
            } else {
                Pair(file.name, file.readBytes())
            }
        } catch (e: Exception) {
            null
        }

    /** Resumo da ROM para explicar uma recusa do núcleo (tamanho e, no NES, cabeçalho iNES e mapper). */
    fun describe(data: ByteArray, name: String, system: String): String {
        val sb = StringBuilder("Arquivo: ").append(name).append(" (").append(data.size).append(" bytes)")
        if (system == "nes") {
            val ines = data.size >= 16 && data[0] == 0x4E.toByte() && data[1] == 0x45.toByte() &&
                data[2] == 0x53.toByte() && data[3] == 0x1A.toByte()
            sb.append("\nCabeçalho iNES: ").append(if (ines) "sim" else "NÃO (arquivo não parece uma ROM de NES)")
            if (ines) {
                val mapper = (data[7].toInt() and 0xF0) or ((data[6].toInt() and 0xFF) shr 4)
                sb.append("\nMapper: ").append(mapper)
                    .append(" · PRG ").append(data[4].toInt() and 0xFF).append("x16 KB")
                    .append(" · CHR ").append(data[5].toInt() and 0xFF).append("x8 KB")
            }
        }
        return sb.toString()
    }

    /** Super Nintendo: tira o cabeçalho de 512 bytes que algumas ROMs antigas têm. NES: não mexe (o iNES precisa do cabeçalho). */
    fun stripHeader(data: ByteArray, system: String): ByteArray =
        if (system == "snes" && data.size % 1024 == 512) data.copyOfRange(512, data.size) else data
}

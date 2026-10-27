package br.hy300.games

import java.io.File
import java.util.zip.ZipFile

object RomLoader {
    private val ROM_EXT = listOf("sfc", "smc", "swc", "fig")

    private fun isRom(name: String) = ROM_EXT.contains(name.substringAfterLast('.', "").lowercase())

    /** O .zip tem alguma ROM de Super Nintendo? (ROMs de NES, por exemplo, ficam de fora.) */
    fun zipHasSnesRom(file: File): Boolean =
        try {
            ZipFile(file).use { zip -> zip.entries().asSequence().any { !it.isDirectory && isRom(it.name) } }
        } catch (e: Exception) {
            false
        }

    /** Devolve (nome, bytes) da ROM. Se for .zip, extrai a primeira ROM de dentro. */
    fun readRom(file: File): Pair<String, ByteArray>? =
        try {
            if (file.extension.lowercase() == "zip") {
                ZipFile(file).use { zip ->
                    val entry = zip.entries().asSequence().firstOrNull { !it.isDirectory && isRom(it.name) }
                    if (entry == null) null else zip.getInputStream(entry).use { Pair(entry.name, it.readBytes()) }
                }
            } else {
                Pair(file.name, file.readBytes())
            }
        } catch (e: Exception) {
            null
        }

    /** Tira o cabeçalho de 512 bytes que algumas ROMs antigas têm. */
    fun stripHeader(data: ByteArray): ByteArray =
        if (data.size % 1024 == 512) data.copyOfRange(512, data.size) else data
}

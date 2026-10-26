package br.hy300.games

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import java.io.File
import java.io.FileNotFoundException

/** Entrega uma ROM ao emulador (somente leitura), com acesso temporário concedido pela intent. */
class RomProvider : ContentProvider() {

    companion object {
        const val AUTHORITY = "br.hy300.games.roms"

        /** content://br.hy300.games.roms/<origem>/<caminho relativo>; o caminho termina no nome do arquivo (.sfc). */
        fun uriFor(source: String, rel: String): Uri {
            val builder = Uri.Builder().scheme("content").authority(AUTHORITY).appendPath(source)
            for (part in rel.split("/")) {
                if (part.isNotEmpty()) builder.appendPath(part)
            }
            return builder.build()
        }
    }

    override fun onCreate(): Boolean = true

    private fun fileFor(uri: Uri): File? {
        val parts = uri.pathSegments
        if (parts.size < 2) return null
        val ctx = context ?: return null
        val source = parts[0]
        if (source != "pub" && source != "app") return null
        val base = Catalog.baseFor(ctx, source)
        val file = File(base, parts.drop(1).joinToString("/"))
        // Não deixa sair da pasta dos jogos
        val inside = file.canonicalPath.startsWith(base.canonicalPath + File.separator)
        return if (inside && file.isFile) file else null
    }

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
        val file = fileFor(uri) ?: throw FileNotFoundException(uri.toString())
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    }

    override fun query(
        uri: Uri, projection: Array<out String>?, selection: String?,
        selectionArgs: Array<out String>?, sortOrder: String?
    ): Cursor? {
        val file = fileFor(uri) ?: return null
        val cursor = MatrixCursor(arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE))
        cursor.addRow(arrayOf<Any>(file.name, file.length()))
        return cursor
    }

    override fun getType(uri: Uri): String? = "application/octet-stream"
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
}

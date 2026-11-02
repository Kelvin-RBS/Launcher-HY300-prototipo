package br.hy300.games

import android.content.Context
import java.io.File
import java.io.InputStream

/** Liga o servidor e a tela "Gerenciar jogos" aos jogos reais do aparelho. */
class AppGameStore(private val ctx: Context) : GameStore {
    private val saves = File(ctx.filesDir, "saves")
    private val states = File(ctx.filesDir, "states")

    private fun idOf(g: Game) = g.source + ":" + g.rel
    private fun find(id: String): Game? = Catalog.games(ctx).firstOrNull { idOf(it) == id }

    override fun list(): List<GameInfo> =
        Catalog.games(ctx).map { GameInfo(idOf(it), it.title, it.file.name, it.systemLabel, it.file.length()) }

    override fun delete(id: String, withSaves: Boolean): Boolean? {
        val g = find(id) ?: return null
        return deleteGame(g, withSaves)
    }

    override fun rename(id: String, newBase: String): GameFiles.Outcome? {
        val g = find(id) ?: return null
        return renameGame(g, newBase)
    }

    override fun upload(fileName: String, input: InputStream, length: Long): GameFiles.Outcome {
        // Sem acesso a todos os arquivos, o jogo vai para a pasta do próprio app
        val root = if (Catalog.canReadPublic()) File(Catalog.PUBLIC_DIR) else Catalog.appRoot(ctx)
        return GameFiles.saveUpload(root, fileName, input, length)
    }

    fun deleteGame(g: Game, withSaves: Boolean): Boolean = GameFiles.delete(g.file, withSaves, saves, states)

    fun renameGame(g: Game, newBase: String): GameFiles.Outcome = GameFiles.rename(g.file, newBase, saves, states)
}

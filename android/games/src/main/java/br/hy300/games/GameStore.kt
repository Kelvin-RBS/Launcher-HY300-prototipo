package br.hy300.games

import java.io.InputStream

/**
 * Tipos e interface compartilhados entre o servidor web (removido) e a implementação local.
 * Mantidos aqui para o AppGameStore continuar compilando.
 */

data class GameInfo(
    val id: String,
    val title: String,
    val file: String,
    val system: String,
    val size: Long
)

/** O que a tela "Gerenciar jogos" precisa saber fazer com os jogos do aparelho. */
interface GameStore {
    fun list(): List<GameInfo>
    /** null = jogo não encontrado. */
    fun delete(id: String, withSaves: Boolean): Boolean?
    /** null = jogo não encontrado. */
    fun rename(id: String, newBase: String): GameFiles.Outcome?
    fun upload(fileName: String, input: InputStream, length: Long): GameFiles.Outcome
}
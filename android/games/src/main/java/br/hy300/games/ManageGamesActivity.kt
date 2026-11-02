package br.hy300.games

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.security.SecureRandom
import java.util.Locale

/**
 * Gerenciar jogos: renomear, excluir (o arquivo é apagado por completo) e receber jogos pelo
 * celular ou computador. O servidor de envio só fica ligado enquanto esta tela está aberta e exige
 * o PIN mostrado aqui.
 */
class ManageGamesActivity : NavActivity() {

    private lateinit var store: AppGameStore
    private lateinit var infoText: TextView
    private lateinit var listBox: LinearLayout
    private lateinit var backButton: TextView
    private var server: GameServer? = null
    private val pin = String.format(Locale.US, "%04d", SecureRandom().nextInt(10000))
    private val ui = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = AppGameStore(this)
        setContentView(buildUi())
    }

    override fun onResume() {
        super.onResume()
        startServer()
        refreshList(false)
    }

    override fun onPause() {
        super.onPause()
        stopServer()
    }

    private fun label(text: String, size: Float, color: String, top: Int = 0): TextView = TextView(this).apply {
        this.text = text
        textSize = size
        setTextColor(Color.parseColor(color))
        setPadding(0, dp(top), 0, 0)
    }

    private fun buildUi(): ScrollView {
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(43), dp(24), dp(43), dp(24))
        }
        col.addView(label("Gerenciar jogos", 28f, "#EAF1FF"))

        col.addView(label("ENVIAR PELO CELULAR OU COMPUTADOR", 11f, "#A9B8D2", 16))
        infoText = label("", 20f, "#EAF1FF", 4)
        col.addView(infoText)
        col.addView(label("Com o aparelho na mesma rede Wi-Fi, abra o endereço acima no navegador e digite o PIN. " +
            "O envio só fica ligado enquanto esta tela está aberta. Use apenas jogos que você tem o direito de usar.", 13f, "#A9B8D2", 4))

        col.addView(label("JOGOS  (OK abre as opções: renomear ou excluir)", 11f, "#A9B8D2", 16))
        listBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        col.addView(listBox)

        backButton = Ui.focusButton(this, "Voltar") { finish() }
        col.addView(backButton, LinearLayout.LayoutParams(dp(140), ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(18) })

        return ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#060A14"))
            addView(col)
        }
    }

    // ------------------------------------------------------------------ servidor

    private fun startServer() {
        stopServer()
        val warning = if (Catalog.canReadPublic()) "" else
            "\nAcesso a arquivos não liberado: os jogos enviados ficam só dentro deste app (veja Configurações)."
        try {
            val s = GameServer(store, pin, { readPage() }, { ui.post { refreshList(true) } })
            val port = s.start()
            server = s
            val ip = NetInfo.localIpv4()
            infoText.text = if (ip != null) {
                "http://" + ip + ":" + port + "\nPIN: " + pin + warning
            } else {
                "Sem rede. Conecte o projetor ao Wi-Fi ou ao cabo para enviar jogos pelo celular." + warning
            }
        } catch (e: Exception) {
            infoText.text = "Não foi possível abrir o envio pela rede." + warning
        }
    }

    private fun stopServer() {
        server?.stop()
        server = null
    }

    private fun readPage(): String =
        assets.open("gerenciador.html").bufferedReader(Charsets.UTF_8).use { it.readText() }

    // ------------------------------------------------------------------ lista

    private fun humanSize(bytes: Long): String =
        if (bytes >= 1048576L) String.format(Locale("pt", "BR"), "%.1f MB", bytes / 1048576.0)
        else Math.max(1L, bytes / 1024L).toString() + " KB"

    private fun refreshList(restoreFocus: Boolean) {
        quiet()
        val focusedKey = if (restoreFocus) currentFocus?.tag as? String else null
        val backHadFocus = restoreFocus && currentFocus === backButton
        listBox.removeAllViews()
        val games = Catalog.games(this)
        if (games.isEmpty()) {
            listBox.addView(label("Nenhum jogo ainda. Envie pelo celular ou coloque arquivos em " + Catalog.PUBLIC_DIR, 15f, "#A9B8D2", 4))
            backButton.requestFocus()
            return
        }
        var toFocus: View? = null
        for (g in games) {
            val key = g.source + ":" + g.rel
            val row = Ui.focusButton(this, g.title + "   ·   " + g.systemLabel + "  ·  " + humanSize(g.file.length())) {
                showGameMenu(g)
            }
            row.tag = key
            row.gravity = Gravity.START or Gravity.CENTER_VERTICAL
            listBox.addView(row, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(8) })
            if (key == focusedKey) toFocus = row
        }
        if (backHadFocus) backButton.requestFocus() else (toFocus ?: listBox.getChildAt(0))?.requestFocus()
    }

    // ------------------------------------------------------------------ ações

    private fun showGameMenu(g: Game) {
        AlertDialog.Builder(this)
            .setTitle(g.title)
            .setItems(arrayOf("Renomear", "Excluir", "Cancelar")) { _, which ->
                when (which) {
                    0 -> renameGame(g)
                    1 -> confirmDelete(g)
                }
            }
            .show()
    }

    private fun renameGame(g: Game) {
        NameKeyboardDialog(this, "Novo nome (a extensão ." + g.file.extension + " é mantida)", g.file.nameWithoutExtension) { name ->
            val o = store.renameGame(g, name)
            val msg = when (o.result) {
                GameFiles.Result.OK -> "Renomeado."
                GameFiles.Result.EXISTS -> "Já existe um jogo com esse nome."
                GameFiles.Result.INVALID -> "Nome inválido."
                else -> "Não foi possível renomear."
            }
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            refreshList(true)
        }.show()
    }

    private fun confirmDelete(g: Game) {
        val dialog = AlertDialog.Builder(this)
            .setTitle("Excluir \"" + g.title + "\"?")
            .setMessage("O arquivo do jogo será apagado do projetor por completo.")
            .setPositiveButton("Excluir jogo e progresso") { _, _ -> deleteGame(g, true) }
            .setNegativeButton("Excluir só o jogo") { _, _ -> deleteGame(g, false) }
            .setNeutralButton("Cancelar", null)
            .create()
        dialog.show()
        // O foco começa em Cancelar: um OK distraído não apaga nada
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).requestFocus()
    }

    private fun deleteGame(g: Game, withSaves: Boolean) {
        val ok = store.deleteGame(g, withSaves)
        Toast.makeText(this, if (ok) "Jogo excluído." else "Não foi possível excluir o arquivo.", Toast.LENGTH_SHORT).show()
        refreshList(true)
    }
}

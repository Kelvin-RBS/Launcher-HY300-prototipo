package br.hy300.games

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.util.concurrent.Executors

/**
 * Gerenciar jogos: renomear, excluir e BAIXAR DO GITHUB (bsnesemulator/SNES-ROMS).
 * Busca catálogo da API do GitHub na primeira vez, faz cache local.
 * Botão "Atualizar" só re-baixa se houver novidade no repo.
 */
class ManageGamesActivity : NavActivity() {

    private lateinit var store: AppGameStore
    private lateinit var listBox: LinearLayout
    private lateinit var backButton: TextView
    private lateinit var statusText: TextView
    private lateinit var searchButton: TextView
    private lateinit var updateButton: TextView
    private val ui = Handler(Looper.getMainLooper())
    private val ioExecutor = Executors.newSingleThreadExecutor()

    // Estado do download do GitHub
    private var catalog: GitHubCatalog.GameCatalog? = null
    private var isLoadingCatalog = false
    private var isDownloading = false
    private var selectedGameIds: MutableSet<String> = mutableSetOf()

    // ------------------------------------------------------------------ Helpers UI

    private fun label(text: String, size: Float, color: String, top: Int = 0): TextView = TextView(this).apply {
        this.text = text
        textSize = size
        setTextColor(Color.parseColor(color))
        setPadding(0, dp(top), 0, 0)
    }

    private fun humanSize(bytes: Long): String =
        if (bytes >= 1048576L) String.format(java.util.Locale("pt", "BR"), "%.1f MB", bytes / 1048576.0)
        else Math.max(1L, bytes / 1024L).toString() + " KB"

    // ------------------------------------------------------------------ Lista local (definida antes para poder ser chamada)

    private fun refreshList(restoreFocus: Boolean) {
        quiet()
        val focusedKey = if (restoreFocus) currentFocus?.tag as? String else null
        val backHadFocus = restoreFocus && currentFocus === backButton
        listBox.removeAllViews()
        val games = Catalog.games(this)
        if (games.isEmpty()) {
            listBox.addView(label("Nenhum jogo ainda. Baixe do GitHub ou coloque arquivos em " + Catalog.PUBLIC_DIR, 15f, "#A9B8D2", 4))
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

    // ------------------------------------------------------------------ Ações (renomear, excluir)

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
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).requestFocus()
    }

    private fun deleteGame(g: Game, withSaves: Boolean) {
        val ok = store.deleteGame(g, withSaves)
        Toast.makeText(this, if (ok) "Jogo excluído." else "Não foi possível excluir o arquivo.", Toast.LENGTH_SHORT).show()
        refreshList(true)
    }

    // ------------------------------------------------------------------ Catálogo GitHub (bsnesemulator/SNES-ROMS)

    private fun loadCatalog(forceUpdate: Boolean = false) {
        isLoadingCatalog = true
        searchButton.isEnabled = false
        updateButton.isEnabled = false
        searchButton.text = if (forceUpdate) "Atualizando..." else "Buscando catálogo..."
        statusText.text = "Conectando ao GitHub (bsnesemulator/SNES-ROMS)..."
        statusText.setTextColor(Color.parseColor("#A9B8D2"))

        ioExecutor.execute {
            val result = GitHubCatalog.fetchCatalog(this, forceUpdate)
            ui.post {
                isLoadingCatalog = false
                if (result != null && result.games.isNotEmpty()) {
                    catalog = result
                    showGameSelectionDialog()
                } else {
                    statusText.text = "Falha ao buscar catálogo. Verifique a rede (a API do GitHub limita 60 consultas/hora sem token) e tente novamente."
                    statusText.setTextColor(Color.parseColor("#EF5B5B"))
                    searchButton.isEnabled = true
                    updateButton.isEnabled = true
                    searchButton.text = "Buscar catálogo"
                    updateButton.text = "Atualizar"
                }
            }
        }
    }

    private fun showGameSelectionDialog() {
        val cat = catalog ?: return
        val all = cat.games
        var visible: List<GitHubCatalog.GameInfo> = all
        var query = ""

        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(8), dp(16), dp(8))
        }
        val header = TextView(this).apply {
            textSize = 15f
            setTextColor(Color.parseColor("#EAF1FF"))
            setPadding(0, 0, 0, dp(8))
        }
        box.addView(header)

        var dialog: AlertDialog? = null
        lateinit var listAdapter: android.widget.BaseAdapter
        lateinit var searchBtn: TextView
        lateinit var selectAllBtn: TextView

        fun updateTexts() {
            header.text = "${visible.size} de ${all.size} jogos  ·  ${selectedGameIds.size} selecionado(s)  ·  commit ${cat.version.take(7)}"
            searchBtn.text = if (query.isEmpty()) "Buscar por nome..." else "Busca: \"$query\"  (OK para mudar)"
            val allSelected = visible.isNotEmpty() && visible.all { selectedGameIds.contains(it.id) }
            selectAllBtn.text = if (allSelected) "Desmarcar os ${visible.size} da lista" else "Selecionar os ${visible.size} da lista"
            dialog?.getButton(AlertDialog.BUTTON_POSITIVE)?.text = "Baixar (${selectedGameIds.size})"
        }

        fun applyFilter() {
            val q = query.lowercase().trim()
            visible = if (q.isEmpty()) all else all.filter {
                it.title.lowercase().contains(q) || it.fileName.lowercase().contains(q)
            }
            listAdapter.notifyDataSetChanged()
            updateTexts()
        }

        searchBtn = Ui.focusButton(this, "Buscar por nome...") {
            NameKeyboardDialog(this, "Buscar jogo (nome ou parte dele)", query) { text ->
                query = text
                applyFilter()
            }.show()
        }
        box.addView(searchBtn, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = dp(6) })

        selectAllBtn = Ui.focusButton(this, "Selecionar todos") {
            val allSelected = visible.isNotEmpty() && visible.all { selectedGameIds.contains(it.id) }
            if (allSelected) visible.forEach { selectedGameIds.remove(it.id) }
            else visible.forEach { selectedGameIds.add(it.id) }
            listAdapter.notifyDataSetChanged()
            updateTexts()
        }
        box.addView(selectAllBtn, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = dp(6) })

        // ListView só cria as linhas que aparecem na tela (o repo tem milhares de jogos)
        listAdapter = object : android.widget.BaseAdapter() {
            override fun getCount() = visible.size
            override fun getItem(position: Int) = visible[position]
            override fun getItemId(position: Int) = position.toLong()
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val tv = (convertView as? TextView) ?: TextView(this@ManageGamesActivity).apply {
                    textSize = 15f
                    setTextColor(Color.parseColor("#EAF1FF"))
                    setPadding(dp(10), dp(10), dp(10), dp(10))
                }
                val g = visible[position]
                val mark = if (selectedGameIds.contains(g.id)) "\u2611" else "\u2610"
                tv.text = "$mark  ${g.title}  ·  ${Systems.label(g.system)}  ·  ${GitHubCatalog.humanSize(g.size)}"
                return tv
            }
        }
        val listView = android.widget.ListView(this).apply {
            adapter = listAdapter
            selector = android.graphics.drawable.ColorDrawable(Color.parseColor("#2F7CF6"))
            divider = android.graphics.drawable.ColorDrawable(Color.parseColor("#1E2C45"))
            dividerHeight = 1
            isFocusable = true
            setOnItemClickListener { _, _, position, _ ->
                val id = visible[position].id
                if (!selectedGameIds.remove(id)) selectedGameIds.add(id)
                listAdapter.notifyDataSetChanged()
                updateTexts()
            }
        }
        box.addView(listView, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            (resources.displayMetrics.heightPixels * 0.5f).toInt()
        ))

        dialog = AlertDialog.Builder(this)
            .setTitle("Baixar jogos do GitHub")
            .setView(box)
            .setPositiveButton("Baixar (${selectedGameIds.size})") { _, _ ->
                if (selectedGameIds.isNotEmpty()) downloadSelectedGames()
            }
            .setNegativeButton("Cancelar", null)
            .setOnDismissListener {
                // Ao tocar em "Baixar" a janela fecha DEPOIS de o download começar: não desfaz o estado de download
                if (isDownloading) return@setOnDismissListener
                searchButton.isEnabled = true
                updateButton.isEnabled = true
                searchButton.text = "Buscar catálogo"
                updateButton.text = "Atualizar"
                statusText.text = "Catálogo carregado (${all.size} jogos). Selecione e toque em Baixar."
                statusText.setTextColor(Color.parseColor("#EAF1FF"))
            }
            .create()
        dialog?.show()
        updateTexts()
        searchBtn.requestFocus()
    }

    private fun downloadSelectedGames() {
        val gamesToDownload = catalog!!.games.filter { selectedGameIds.contains(it.id) }
        if (gamesToDownload.isEmpty()) return

        isDownloading = true
        searchButton.isEnabled = false
        updateButton.isEnabled = false
        searchButton.text = "Baixando..."
        selectedGameIds.clear()

        // Baixa em background sequencialmente
        ioExecutor.execute {
            var successCount = 0
            var failCount = 0

            for ((index, game) in gamesToDownload.withIndex()) {
                if (!isDownloading) break // Cancelado

                ui.post {
                    statusText.text = "Baixando ${index + 1}/${gamesToDownload.size}: ${game.title}"
                    statusText.setTextColor(Color.parseColor("#A9B8D2"))
                }

                val outcome = GitHubCatalog.downloadGame(this, game) { progress ->
                    ui.post {
                        statusText.text = "Baixando ${index + 1}/${gamesToDownload.size}: ${game.title}  ·  ${(progress * 100).toInt()}%"
                    }
                }

                if (outcome.result == GameFiles.Result.OK) {
                    successCount++
                } else if (outcome.result == GameFiles.Result.EXISTS) {
                    // Já existe, conta como sucesso
                    successCount++
                } else {
                    failCount++
                }
            }

            ui.post {
                isDownloading = false
                searchButton.isEnabled = true
                updateButton.isEnabled = true
                searchButton.text = "Buscar catálogo"
                updateButton.text = "Atualizar"

                if (successCount > 0 && failCount == 0) {
                    statusText.text = "$successCount jogo(s) baixado(s) com sucesso!"
                    statusText.setTextColor(Color.parseColor("#3ECF8E"))
                } else if (successCount > 0 && failCount > 0) {
                    statusText.text = "$successCount baixado(s), $failCount falhou."
                    statusText.setTextColor(Color.parseColor("#EF5B5B"))
                } else {
                    statusText.text = "Falha ao baixar jogos. Verifique a rede."
                    statusText.setTextColor(Color.parseColor("#EF5B5B"))
                }

                // Atualiza a lista local
                refreshList(true)
            }
        }
    }

    // ------------------------------------------------------------------ Ciclo de vida

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = AppGameStore(this)
        setContentView(buildUi())
    }

    override fun onResume() {
        super.onResume()
        refreshList(false)
    }

    override fun onPause() {
        super.onPause()
        // Cancela downloads em andamento se a tela for fechada
        isDownloading = false
    }

    override fun onDestroy() {
        super.onDestroy()
        ioExecutor.shutdownNow()
    }

    private fun buildUi(): ScrollView {
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(43), dp(24), dp(43), dp(24))
        }
        col.addView(label("Gerenciar jogos", 28f, "#EAF1FF"))

        // --- BAIXAR DO GITHUB ---
        col.addView(label("BAIXAR JOGOS DO GITHUB (bsnesemulator/SNES-ROMS)", 11f, "#A9B8D2", 16))
        statusText = label("Toque em \"Buscar catálogo\" para ver os jogos disponíveis.", 15f, "#EAF1FF", 4)
        col.addView(statusText)

        // Linha de botões: Buscar + Atualizar
        val buttonRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        searchButton = Ui.focusButton(this, "Buscar catálogo") {
            if (!isLoadingCatalog && !isDownloading) {
                loadCatalog(false)
            }
        }
        buttonRow.addView(searchButton, LinearLayout.LayoutParams(dp(220), ViewGroup.LayoutParams.WRAP_CONTENT).apply { marginEnd = dp(12) })

        updateButton = Ui.focusButton(this, "Atualizar") {
            if (!isLoadingCatalog && !isDownloading) {
                // Checa se há novidade antes de buscar
                ioExecutor.execute {
                    val hasUpdate = GitHubCatalog.checkForUpdate(this@ManageGamesActivity)
                    ui.post {
                        if (hasUpdate == null) {
                            statusText.text = "Não foi possível verificar agora (sem rede ou limite do GitHub). Tente de novo mais tarde."
                            statusText.setTextColor(Color.parseColor("#EF5B5B"))
                        } else if (hasUpdate) {
                            statusText.text = "Nova versão no repo. Baixando catálogo atualizado..."
                            statusText.setTextColor(Color.parseColor("#A9B8D2"))
                            loadCatalog(true)
                        } else {
                            statusText.text = "Catálogo já está atualizado (commit ${catalog?.version?.take(7) ?: "?"})."
                            statusText.setTextColor(Color.parseColor("#3ECF8E"))
                        }
                    }
                }
            }
        }
        updateButton.isEnabled = false // habilita só depois de carregar
        buttonRow.addView(updateButton, LinearLayout.LayoutParams(dp(160), ViewGroup.LayoutParams.WRAP_CONTENT))

        col.addView(buttonRow, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(8) })

        col.addView(label("Os jogos são baixados do repo bsnesemulator/SNES-ROMS para o projetor. " +
            "Use apenas jogos que você tem o direito de usar.", 13f, "#A9B8D2", 4))

        // --- LISTA DE JOGOS LOCAIS ---
        col.addView(label("JOGOS NO PROJETOR  (OK abre as opções: renomear ou excluir)", 11f, "#A9B8D2", 16))
        listBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        col.addView(listBox)

        backButton = Ui.focusButton(this, "Voltar") { finish() }
        col.addView(backButton, LinearLayout.LayoutParams(dp(140), ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(18) })

        return ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#060A14"))
            addView(col)
        }
    }
}
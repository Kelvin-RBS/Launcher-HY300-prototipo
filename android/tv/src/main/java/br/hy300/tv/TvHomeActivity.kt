package br.hy300.tv

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.TextUtils
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextClock
import android.widget.TextView
import android.widget.Toast

/**
 * Tela principal: categorias à esquerda, canais no meio e ficha do canal à direita.
 * Navegação só com o controle remoto.
 */
class TvHomeActivity : Activity() {

    companion object {
        const val GROUP_ALL = "Todos"
        const val GROUP_FAV = "Favoritos"

        /** Última linha da lista de categorias: abre a configuração da fonte. */
        const val GROUP_SOURCE = "⚙ Fonte"
    }

    private lateinit var body: FrameLayout
    private lateinit var groupList: ListView
    private lateinit var channelList: ListView
    private lateinit var originView: TextView
    private lateinit var infoBox: LinearLayout
    private lateinit var hintView: TextView

    private var groups: List<String> = listOf(GROUP_ALL, GROUP_FAV)
    private var shown: List<Channel> = emptyList()
    private var favorites: MutableSet<String> = LinkedHashSet()
    private var selectedGroup: String = GROUP_ALL
    private var lastKey: String? = null
    private var suppressGroupSelect = false
    private var shownStart = 0
    private var seenVersion = Library.version

    /** Qual coluna estava com o foco: true = canais, false = categorias. */
    private var channelsFocused = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyIntentSource(intent)
        setContentView(buildUi())
        loadList(first = true)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (applyIntentSource(intent)) loadList(first = true)
    }

    /**
     * Aceita a lista por link: abrir o app com `am start -d <url>` já salva a fonte.
     * Serve para mandar a lista pelo PC sem digitar no controle.
     */
    private fun applyIntentSource(intent: Intent?): Boolean {
        val url = intent?.data?.toString() ?: return false
        if (!url.startsWith("http://", true) && !url.startsWith("https://", true)) return false
        this.intent?.data = null
        Source.setMode(this, Source.MODE_URL)
        Source.setUrl(this, url)
        Library.dirty = true
        android.widget.Toast.makeText(this, "Lista recebida pelo link", android.widget.Toast.LENGTH_SHORT).show()
        return true
    }

    override fun onResume() {
        super.onResume()
        hideSystemBars()
        favorites = Favorites.load(this)
        lastKey = LastChannel.get(this)
        // Voltou da configuração com fonte/filtro novo: recarrega de verdade.
        if (Library.dirty) {
            loadList(first = true)
            return
        }
        // A fonte foi salva em outra tela: redesenha com a lista nova.
        if (Library.version != seenVersion) {
            seenVersion = Library.version
            if (Library.channels.isEmpty()) loadList(first = true) else rebuild()
            return
        }
        if (Library.channels.isNotEmpty()) rebuild()
    }

    @Suppress("DEPRECATION")
    private fun hideSystemBars() {
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            startActivity(Intent(this, SourceActivity::class.java))
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    // ------------------------------------------------------------------ interface

    private fun buildUi(): View {
        val mp = ViewGroup.LayoutParams.MATCH_PARENT
        val wc = ViewGroup.LayoutParams.WRAP_CONTENT

        val root = FrameLayout(this).apply {
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(Color.parseColor("#0A1526"), Color.parseColor(Ui.BG_DARK))
            )
        }

        // Topo
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(26), dp(18), dp(26), 0)
        }
        header.addView(Ui.label(this, "TV AO VIVO", 17f, Ui.TEXT).apply { letterSpacing = 0.22f })
        originView = Ui.label(this, "carregando…", 10f, Ui.TEXT_MUTED).apply {
            setPadding(0, dp(2), 0, 0)
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.MIDDLE
        }
        header.addView(originView)
        root.addView(header, FrameLayout.LayoutParams(wc, wc, Gravity.TOP or Gravity.START))

        root.addView(TextClock(this).apply {
            setTextColor(Color.parseColor(Ui.TEXT_DIM))
            textSize = 14f
        }, FrameLayout.LayoutParams(wc, wc, Gravity.TOP or Gravity.END).apply {
            topMargin = dp(20); marginEnd = dp(26)
        })

        // Corpo
        body = FrameLayout(this)
        root.addView(body, FrameLayout.LayoutParams(mp, mp).apply {
            topMargin = dp(64)
            bottomMargin = dp(40)
        })

        hintView = Ui.label(
            this,
            "OK abre o canal  ·  segure OK favorita  ·  ⚙ Fonte no fim das categorias",
            10f,
            Ui.TEXT_MUTED
        )
        root.addView(hintView, FrameLayout.LayoutParams(wc, wc, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL).apply {
            bottomMargin = dp(12)
        })

        return root
    }

    private fun buildListUi() {
        body.removeAllViews()
        val mp = ViewGroup.LayoutParams.MATCH_PARENT
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }

        // Categorias
        groupList = ListView(this).apply {
            divider = null
            dividerHeight = 0
            selector = Ui.rounded(this@TvHomeActivity, Ui.FOCUS, 6)
            setPadding(dp(6), dp(4), dp(6), dp(4))
            clipToPadding = false
            isFocusable = true
        }
        row.addView(groupList, LinearLayout.LayoutParams(dp(156), mp).apply { marginStart = dp(18) })
        groupList.setOnFocusChangeListener { _, has -> if (has) channelsFocused = false }

        // Canais
        channelList = ListView(this).apply {
            divider = null
            dividerHeight = 0
            selector = Ui.rounded(this@TvHomeActivity, Ui.FOCUS, 6)
            setPadding(dp(4), dp(4), dp(4), dp(4))
            clipToPadding = false
            isFocusable = true
        }
        row.addView(channelList, LinearLayout.LayoutParams(0, mp, 1f).apply {
            marginStart = dp(8)
            marginEnd = dp(8)
        })
        channelList.setOnFocusChangeListener { _, has -> if (has) channelsFocused = true }

        // Ficha do canal
        val panel = FrameLayout(this).apply {
            background = Ui.rounded(this@TvHomeActivity, Ui.CARD, 10, 1, Ui.CARD_LINE)
        }
        infoBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(16), dp(14), dp(14))
        }
        panel.addView(infoBox, FrameLayout.LayoutParams(mp, mp))
        row.addView(panel, LinearLayout.LayoutParams(dp(226), mp).apply { marginEnd = dp(20) })

        body.addView(row, FrameLayout.LayoutParams(mp, mp))
        wireListNavigation()
    }

    private fun wireListNavigation() {
        groupList.onItemClickListener = object : android.widget.AdapterView.OnItemClickListener {
            override fun onItemClick(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                val g = groups.getOrElse(position) { GROUP_ALL }
                if (g == GROUP_SOURCE) {
                    startActivity(Intent(this@TvHomeActivity, SourceActivity::class.java))
                    return
                }
                selectedGroup = g
                fillChannels()
                channelList.requestFocus()
            }
        }
        groupList.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                // Durante a montagem da tela o ListView dispara seleções sozinho; ignorar.
                if (suppressGroupSelect) return
                val g = groups.getOrElse(position) { GROUP_ALL }
                if (g == GROUP_SOURCE) return
                selectedGroup = g
                fillChannels()
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        channelList.onItemClickListener = object : android.widget.AdapterView.OnItemClickListener {
            override fun onItemClick(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                val ch = shown.getOrNull(position) ?: return
                play(ch)
            }
        }
        channelList.onItemLongClickListener = object : android.widget.AdapterView.OnItemLongClickListener {
            override fun onItemLongClick(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long): Boolean {
                val ch = shown.getOrNull(position) ?: return false
                val added = Favorites.toggle(this@TvHomeActivity, ch)
                favorites = Favorites.load(this@TvHomeActivity)
                toast(if (added) "Adicionado aos favoritos" else "Removido dos favoritos")
                fillChannels(keepPosition = position)
                return true
            }
        }
        channelList.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                showInfo(shown.getOrNull(position))
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        // ◀ da lista de canais volta para as categorias; ▶ das categorias vai para os canais
        channelList.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                groupList.requestFocus(); true
            } else false
        }
        groupList.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                channelList.requestFocus(); true
            } else false
        }
    }

    private fun showSourceUi(message: String, showButton: Boolean) {
        body.removeAllViews()
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = Ui.rounded(this@TvHomeActivity, Ui.CARD, 12, 1, Ui.CARD_LINE)
            setPadding(dp(28), dp(24), dp(28), dp(24))
        }
        box.addView(Ui.label(this, "Nenhum canal ainda", 20f, Ui.TEXT))
        box.addView(Ui.label(this, message, 12f, Ui.TEXT_DIM).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(10), 0, dp(16))
            maxLines = 4
        })
        if (showButton) {
            val b = button("Configurar a fonte") { startActivity(Intent(this, SourceActivity::class.java)) }
            box.addView(b)
        }
        body.addView(box, FrameLayout.LayoutParams(dp(460), ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER))
        if (showButton) {
            body.post { box.findFocus()?.requestFocus() ?: box.getChildAt(2)?.requestFocus() }
        }
    }

    private fun button(text: String, onClick: () -> Unit): TextView {
        val bg = Ui.rounded(this, Ui.CARD, 10, 2, "#3A4B6B")
        val b = Ui.label(this, text, 15f, Ui.TEXT_DIM)
        b.apply {
            gravity = Gravity.CENTER
            setPadding(dp(20), dp(12), dp(20), dp(12))
            background = bg
            isFocusable = true
            isClickable = true
            setOnClickListener { onClick() }
        }
        b.setOnFocusChangeListener { _, has ->
            bg.setColor(Color.parseColor(if (has) Ui.FOCUS else Ui.CARD))
            bg.setStroke(dp(if (has) 3 else 2), Color.parseColor(if (has) "#FFFFFF" else "#3A4B6B"))
            b.setTextColor(Color.parseColor(if (has) "#FFFFFF" else Ui.TEXT_DIM))
        }
        return b
    }

    // ------------------------------------------------------------------ dados

    private fun originLabel(): String {
        val base = Library.origin ?: Source.describe(this)
        if (Library.channels.isEmpty()) return base
        val n = Library.channels.size
        val extra = if (Library.heavyDropped > 0) "  ·  $n canais (${Library.heavyDropped} 4K/8K fora)" else "  ·  $n canais"
        return base + extra
    }

    private fun loadList(first: Boolean) {
        originView.text = Source.describe(this)
        if (!first && Library.channels.isNotEmpty()) return
        body.removeAllViews()
        body.addView(Ui.label(this, "Carregando a lista…", 14f, Ui.TEXT_DIM).apply {
            gravity = Gravity.CENTER
        }, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

        Thread {
            Library.reload(this)
            runOnUiThread {
                if (isFinishing) return@runOnUiThread
                originView.text = originLabel()
                if (Library.channels.isEmpty()) {
                    showSourceUi(Library.error ?: "Configure de onde vêm os canais.", true)
                } else {
                    rebuild()
                    seenVersion = Library.version
                    // Só depois do layout a ListView aceita a seleção inicial de verdade.
                    channelList.post {
                        channelList.setSelection(shownStart)
                        groupList.setSelectionFromTop(groups.indexOf(selectedGroup).coerceAtLeast(0), 0)
                        channelsFocused = true
                        // Com listas grandes o foco só pega depois de um segundo ciclo.
                        channelList.postDelayed({ if (!isFinishing) channelList.requestFocus() }, 120)
                    }
                }
            }
        }.start()
    }

    private fun rebuild() {
        buildListUi()
        favorites = Favorites.load(this)
        lastKey = LastChannel.get(this)
        groups = listOf(GROUP_ALL, GROUP_FAV) + Library.groups() + GROUP_SOURCE
        if (selectedGroup !in groups) selectedGroup = GROUP_ALL
        suppressGroupSelect = true
        groupList.adapter = GroupAdapter(this, groups)
        groupList.setSelection(groups.indexOf(selectedGroup).coerceAtLeast(0))
        fillChannels()
        originView.text = originLabel()
        // Depois do layout, religa as seleções e garante a categoria atual marcada.
        groupList.post {
            val i = groups.indexOf(selectedGroup).coerceAtLeast(0)
            groupList.setSelectionFromTop(i, 0)
            suppressGroupSelect = false
            // Recriar a tela perde o foco: devolve para a coluna onde o usuário estava.
            if (channelsFocused) channelList.requestFocus() else groupList.requestFocus()
        }
    }

    private fun fillChannels(keepPosition: Int = -1) {
        val fav = favorites
        shown = when (selectedGroup) {
            GROUP_ALL -> Library.channels
            GROUP_FAV -> Library.channels.filter { Favorites.isFavorite(fav, it) }
            else -> Library.channels.filter { it.group == selectedGroup }
        }
        channelList.adapter = ChannelAdapter(this, shown, fav, lastKey)
        val start = when {
            keepPosition in shown.indices -> keepPosition
            else -> shown.indexOfFirst { lastKey != null && it.name + "|" + it.url == lastKey }
                .coerceAtLeast(0)
        }
        if (shown.isNotEmpty()) {
            channelList.setSelection(start)
            shownStart = start
            showInfo(shown[start])
        } else {
            shownStart = 0
            showInfo(null)
        }
    }

    private fun showInfo(ch: Channel?) {
        infoBox.removeAllViews()
        if (ch == null) {
            infoBox.addView(Ui.label(this, "Nenhum canal nesta categoria", 13f, Ui.TEXT_MUTED))
            return
        }
        val mark = Ui.label(this, ch.name.trim().firstOrNull()?.uppercase() ?: "#", 30f, Ui.TEXT).apply {
            gravity = Gravity.CENTER
            background = Ui.rounded(this@TvHomeActivity, "#0E1626", 8, 1, Ui.CARD_LINE)
        }
        infoBox.addView(mark, LinearLayout.LayoutParams(dp(64), dp(64)).apply { bottomMargin = dp(12) })
        infoBox.addView(Ui.label(this, ch.number.toString(), 12f, Ui.TEXT_MUTED))
        infoBox.addView(Ui.label(this, ch.name, 17f, Ui.TEXT).apply { maxLines = 3 })
        infoBox.addView(Ui.label(this, ch.group, 11f, Ui.TEXT_DIM).apply {
            setPadding(0, dp(4), 0, dp(10))
            maxLines = 2
        })
        infoBox.addView(Ui.liveBadge(this))
        val fav = Favorites.isFavorite(Favorites.load(this), ch)
        infoBox.addView(Ui.label(this, if (fav) "\u2605 Favorito" else "\u2606 Segure OK p/ favoritar", 10f,
            if (fav) "#FFD166" else Ui.TEXT_MUTED).apply { setPadding(0, dp(12), 0, 0) })
    }

    private fun play(ch: Channel) {
        val index = Library.channels.indexOfFirst { it.number == ch.number && it.url == ch.url }
        if (index < 0) return
        startActivity(Intent(this, PlayerActivity::class.java).putExtra(PlayerActivity.EXTRA_INDEX, index))
    }

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}

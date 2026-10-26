package br.hy300.games

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextClock
import android.widget.TextView
import android.widget.Toast

/** Tela inicial do app de jogos: visual do launcher, engrenagem no topo e a lista com o nome dos jogos. */
class GamesHomeActivity : Activity() {

    private lateinit var titleView: TextView
    private lateinit var descView: TextView
    private lateinit var list: LinearLayout
    private lateinit var gear: View
    private var firstRow: View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildUi())
    }

    override fun onResume() {
        super.onResume()
        hideSystemBars()
        rebuildList()
    }

    @Suppress("DEPRECATION")
    private fun hideSystemBars() {
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
    }

    private fun buildUi(): View {
        val mp = ViewGroup.LayoutParams.MATCH_PARENT
        val wc = ViewGroup.LayoutParams.WRAP_CONTENT
        val root = FrameLayout(this)
        root.clipChildren = false
        root.setBackgroundColor(Color.BLACK)

        root.addView(ImageView(this).apply {
            setImageResource(R.drawable.wallpaper)
            scaleType = ImageView.ScaleType.CENTER_CROP
        }, FrameLayout.LayoutParams(mp, mp))
        root.addView(shade(
            GradientDrawable.Orientation.LEFT_RIGHT,
            intArrayOf(Color.argb(224, 4, 8, 16), Color.argb(115, 4, 8, 16), Color.argb(13, 4, 8, 16))
        ), FrameLayout.LayoutParams(mp, mp))
        root.addView(shade(
            GradientDrawable.Orientation.BOTTOM_TOP,
            intArrayOf(Color.argb(235, 4, 8, 16), Color.argb(0, 4, 8, 16))
        ), FrameLayout.LayoutParams(mp, dp(264), Gravity.BOTTOM))

        // Topo: engrenagem (configurações) e hora
        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            clipChildren = false
            clipToPadding = false
            setPadding(dp(8), dp(8), dp(8), dp(8))
        }
        gear = roundButton(R.drawable.ic_gear, "Configurações", "Controles e informações do emulador.") {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        top.addView(gear)
        top.addView(TextClock(this).apply {
            setTextColor(Color.WHITE)
            textSize = 17f
            setPadding(dp(6), 0, 0, 0)
        })
        root.addView(top, FrameLayout.LayoutParams(wc, wc, Gravity.TOP or Gravity.END).apply {
            topMargin = dp(8)
            marginEnd = dp(24)
        })

        // Título e descrição do item em foco
        val info = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        info.addView(TextView(this).apply {
            text = "MEUS JOGOS"
            setTextColor(Color.parseColor("#A9B8D2"))
            textSize = 11f
            letterSpacing = 0.24f
        })
        titleView = TextView(this).apply {
            setTextColor(Color.parseColor("#EAF1FF"))
            textSize = 40f
            maxLines = 2
            setPadding(0, dp(6), 0, dp(6))
        }
        descView = TextView(this).apply {
            setTextColor(Color.parseColor("#D5DFF2"))
            textSize = 16f
        }
        info.addView(titleView)
        info.addView(descView)
        root.addView(info, FrameLayout.LayoutParams(dp(520), wc).apply {
            leftMargin = dp(43)
            topMargin = dp(70)
        })

        // Lista: só o nome de cada jogo
        list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            clipChildren = false
            setPadding(dp(6), dp(6), dp(6), dp(6))
        }
        val scroll = ScrollView(this).apply {
            isVerticalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
            clipToPadding = false
            addView(list)
        }
        root.addView(scroll, FrameLayout.LayoutParams(dp(430), dp(215)).apply {
            leftMargin = dp(37)
            topMargin = dp(232)
        })
        return root
    }

    private fun shade(orientation: GradientDrawable.Orientation, colors: IntArray): View =
        View(this).apply { background = GradientDrawable(orientation, colors) }

    private fun roundButton(icon: Int, name: String, text: String, action: () -> Unit): View {
        val bgd = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.argb(140, 10, 16, 32))
        }
        val v = ImageView(this).apply {
            setImageResource(icon)
            setPadding(dp(8), dp(8), dp(8), dp(8))
            background = bgd
            isFocusable = true
            isClickable = true
            contentDescription = name
            setOnClickListener { action() }
        }
        v.setOnFocusChangeListener { view, has ->
            view.animate().scaleX(if (has) 1.12f else 1f).scaleY(if (has) 1.12f else 1f).setDuration(120).start()
            bgd.setStroke(if (has) dp(2) else 0, Color.rgb(227, 238, 255))
            if (has) {
                titleView.text = name
                descView.text = text
            }
        }
        v.layoutParams = LinearLayout.LayoutParams(dp(34), dp(34)).apply { marginEnd = dp(10) }
        return v
    }

    private fun rebuildList() {
        list.removeAllViews()
        firstRow = null
        val games = Catalog.games(this)
        if (games.isEmpty()) {
            list.addView(TextView(this).apply {
                text = "Nenhum jogo encontrado.\n\nColoque os arquivos .sfc em:\n" + Catalog.PUBLIC_DIR +
                    (if (Catalog.canReadPublic()) "" else "\n\nFalta liberar o acesso a arquivos. Veja em Configurações.")
                textSize = 14f
                setTextColor(Color.parseColor("#D5DFF2"))
            })
            titleView.text = "Jogos"
            descView.text = "Nenhum jogo ainda."
            gear.requestFocus()
            return
        }
        for (g in games) addRow(g)
        firstRow?.requestFocus()
    }

    private fun addRow(g: Game) {
        val bg = GradientDrawable().apply {
            cornerRadius = dp(8).toFloat()
            setColor(Color.argb(110, 10, 16, 32))
        }
        val row = TextView(this).apply {
            text = g.title
            textSize = 18f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), 0, dp(16), 0)
            maxLines = 1
            background = bg
            isFocusable = true
            isClickable = true
            setOnClickListener { play(g) }
        }
        row.setOnFocusChangeListener { _, has ->
            bg.setStroke(if (has) dp(2) else 0, Color.rgb(227, 238, 255))
            bg.setColor(if (has) Color.argb(210, 22, 35, 59) else Color.argb(110, 10, 16, 32))
            if (has) {
                titleView.text = g.title
                descView.text = g.systemLabel + "  \u00B7  OK para jogar"
            }
        }
        list.addView(row, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(44)).apply {
            bottomMargin = dp(8)
        })
        if (firstRow == null) firstRow = row
    }

    /** Ao escolher o jogo: se não houver controle, pede para conectar; se houver, abre o jogo. */
    private fun play(g: Game) {
        if (!Emulator.installed(packageManager)) {
            Toast.makeText(this, "Instale o Snes9x EX+ no projetor", Toast.LENGTH_LONG).show()
            return
        }
        if (Controllers.connected().isEmpty()) {
            startActivity(
                Intent(this, ControllerWaitActivity::class.java)
                    .putExtra("source", g.source)
                    .putExtra("rel", g.rel)
            )
            return
        }
        if (!Emulator.launch(this, g)) {
            Toast.makeText(this, "Não foi possível abrir o jogo", Toast.LENGTH_LONG).show()
        }
    }
}

package br.hy300.tv

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/** Configuração da fonte da lista: arquivo no aparelho, endereço .m3u ou Xtream Codes. */
class SourceActivity : Activity() {

    private lateinit var fields: LinearLayout
    private lateinit var modes: LinearLayout
    private lateinit var status: TextView
    private var mode: String = Source.MODE_FILE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mode = Source.mode(this)
        setContentView(buildUi())
        rebuildFields()
        modes.post { modes.getChildAt(0)?.requestFocus() }
    }

    private fun buildUi(): View {
        val mp = ViewGroup.LayoutParams.MATCH_PARENT
        val wc = ViewGroup.LayoutParams.WRAP_CONTENT

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor(Ui.BG_DARK))
            setPadding(dp(34), dp(22), dp(34), dp(18))
        }

        root.addView(Ui.label(this, "Fonte da lista", 22f, Ui.TEXT))
        root.addView(Ui.label(this, "Escolha de onde o TV AO VIVO lê os canais.", 11f, Ui.TEXT_DIM)
            .apply { setPadding(0, dp(2), 0, dp(14)) })

        // Seletor de modo
        modes = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, dp(12))
        }
        modes.addView(modeButton("Arquivo .m3u", Source.MODE_FILE))
        modes.addView(modeButton("Endereço (URL)", Source.MODE_URL))
        modes.addView(modeButton("Xtream Codes", Source.MODE_XTREAM))
        root.addView(modes)

        val scroll = ScrollView(this)
        fields = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, dp(8))
        }
        scroll.addView(fields, ViewGroup.LayoutParams(mp, wc))
        root.addView(scroll, LinearLayout.LayoutParams(mp, 0, 1f))

        status = Ui.label(this, "", 11f, Ui.TEXT_DIM).apply { maxLines = 2 }
        root.addView(status, LinearLayout.LayoutParams(mp, wc).apply { topMargin = dp(6) })

        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(10), 0, 0)
        }
        actions.addView(action("Testar e salvar", primary = true) { testAndSave() })
        actions.addView(action("Voltar") { finish() })
        root.addView(actions)

        return root
    }

    // ------------------------------------------------------------------ campos

    private fun rebuildFields() {
        fields.removeAllViews()

        // Filtro de qualidade, comum a todas as fontes
        fields.addView(fieldRow(
            "Canais 4K/8K",
            if (Source.cleanHeavy(this)) "Removidos" else "Mantidos"
        ) {
            Source.setCleanHeavy(this, !Source.cleanHeavy(this))
            Library.dirty = true
            status.setTextColor(Color.parseColor(Ui.TEXT_DIM))
            status.text = "Toque em Testar e salvar para aplicar."
            rebuildFields()
        })
        fields.addView(hint(
            "Projetor 720p: SD e HD ficam; 4K/8K só esquenta o decoder e trava."
        ))

        when (mode) {
            Source.MODE_URL -> {
                fields.addView(fieldRow("Endereço .m3u", Source.url(this).ifBlank { "toque para digitar" }) {
                    KeyboardDialog(this, "Endereço da lista (.m3u)", Source.url(this)) { v ->
                        Source.setUrl(this, v)
                        rebuildFields()
                    }.show()
                })
                fields.addView(hint("Aceita http://, https:// e listas com token na URL."))
            }
            Source.MODE_XTREAM -> {
                fields.addView(fieldRow("Servidor", Source.host(this).ifBlank { "ex.: http://meuservidor.com:8080" }) {
                    KeyboardDialog(this, "Servidor Xtream", Source.host(this)) { v ->
                        Source.setXtream(this, v, Source.user(this), Source.pass(this), Source.output(this))
                        rebuildFields()
                    }.show()
                })
                fields.addView(fieldRow("Usuário", Source.user(this).ifBlank { "toque para digitar" }) {
                    KeyboardDialog(this, "Usuário", Source.user(this)) { v ->
                        Source.setXtream(this, Source.host(this), v, Source.pass(this), Source.output(this))
                        rebuildFields()
                    }.show()
                })
                fields.addView(fieldRow("Senha", if (Source.pass(this).isEmpty()) "toque para digitar" else "••••••") {
                    KeyboardDialog(this, "Senha", Source.pass(this)) { v ->
                        Source.setXtream(this, Source.host(this), Source.user(this), v, Source.output(this))
                        rebuildFields()
                    }.show()
                })
                fields.addView(fieldRow("Formato do stream", if (Source.output(this) == "hls") "HLS (.m3u8)" else "TS (.ts)") {
                    Source.setOutput(this, if (Source.output(this) == "hls") "ts" else "hls")
                    rebuildFields()
                })
                fields.addView(hint("Usa get.php?type=m3u_plus. Se o canal não abrir, troque o formato."))
            }
            else -> {
                val found = findLocal()
                fields.addView(fieldRow(
                    "Arquivo encontrado",
                    found ?: "nenhum .m3u em Download"
                ) { testAndSave() })
                fields.addView(hint("Coloque a lista em Download/tvaovivo.m3u. Pelo PC:  adb push lista.m3u /sdcard/Download/tvaovivo.m3u"))
            }
        }
    }

    private fun findLocal(): String? {
        val dl = android.os.Environment.getExternalStoragePublicDirectory(
            android.os.Environment.DIRECTORY_DOWNLOADS
        )
        val direct = java.io.File(dl, "tvaovivo.m3u")
        if (direct.isFile && direct.length() > 0) return direct.name + "  (" + sizeMb(direct.length()) + ")"
        val app = getExternalFilesDir(null)?.let { java.io.File(it, "lista.m3u") }
        if (app != null && app.isFile && app.length() > 0) return "app/lista.m3u  (" + sizeMb(app.length()) + ")"
        val any = dl.listFiles { f ->
            f.isFile && (f.name.endsWith(".m3u", true) || f.name.endsWith(".m3u8", true))
        }?.sortedBy { it.name.lowercase() }?.firstOrNull()
        if (any != null) return any.name + "  (" + sizeMb(any.length()) + ")"
        return null
    }

    private fun sizeMb(bytes: Long): String =
        if (bytes < 1048576) String.format("%.0f KB", bytes / 1024f)
        else String.format("%.1f MB", bytes / 1048576f)

    // ------------------------------------------------------------------ ações

    private fun testAndSave() {
        Source.setMode(this, mode)
        status.setTextColor(Color.parseColor(Ui.TEXT_DIM))
        status.text = "Testando a lista…"
        setBusy(true)
        Thread {
            Library.reload(this)
            runOnUiThread {
                if (isFinishing) return@runOnUiThread
                setBusy(false)
                if (Library.channels.isEmpty()) {
                    status.setTextColor(Color.parseColor("#FF8A8A"))
                    status.text = "Não deu: " + (Library.error ?: "lista vazia")
                } else {
                    status.setTextColor(Color.parseColor("#8BE28B"))
                    val total = Library.totalFound
                    val hd = if (Library.heavyDropped > 0) " (${Library.heavyDropped} em 4K/8K fora; lista tem $total)" else ""
                    status.text = "Pronto: ${Library.channels.size} canais$hd em ${Library.groups().size} categorias." +
                        (Library.note?.let { "  ·  $it" } ?: "")
                }
                rebuildFields()
            }
        }.start()
    }

    private var busy = false

    private fun setBusy(on: Boolean) {
        busy = on
    }

    // ------------------------------------------------------------------ componentes

    private fun modeButton(text: String, value: String): TextView {
        val selected = mode == value
        val bg = Ui.rounded(this, if (selected) "#1D4ED8" else Ui.CARD, 8,
            if (selected) 2 else 1, if (selected) "#7FB0FF" else Ui.CARD_LINE)
        val b = Ui.label(this, text, 13f, if (selected) "#FFFFFF" else Ui.TEXT_DIM).apply {
            gravity = Gravity.CENTER
            setPadding(dp(14), dp(10), dp(14), dp(10))
            background = bg
            isFocusable = true
            isClickable = true
            setOnClickListener {
                if (mode != value) {
                    mode = value
                    Source.setMode(this@SourceActivity, value)
                    Library.dirty = true
                    setContentView(buildUi())
                    rebuildFields()
                    val order = listOf(Source.MODE_FILE, Source.MODE_URL, Source.MODE_XTREAM)
                    modes.post { modes.getChildAt(order.indexOf(value).coerceAtLeast(0))?.requestFocus() }
                }
            }
        }
        b.setOnFocusChangeListener { _, has ->
            if (has) bg.setStroke(dp(3), Color.WHITE) else bg.setStroke(
                dp(if (selected) 2 else 1),
                Color.parseColor(if (selected) "#7FB0FF" else Ui.CARD_LINE)
            )
        }
        b.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            .apply { marginEnd = dp(8) }
        return b
    }

    private fun fieldRow(label: String, value: String, onClick: () -> Unit): View {
        val bg = Ui.rounded(this, Ui.CARD, 8, 1, Ui.CARD_LINE)
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(11), dp(14), dp(11))
            background = bg
            isFocusable = true
            isClickable = true
            setOnClickListener { onClick() }
        }
        row.addView(Ui.label(this, label, 12f, Ui.TEXT_DIM),
            LinearLayout.LayoutParams(dp(150), ViewGroup.LayoutParams.WRAP_CONTENT))
        val v = Ui.label(this, value, 13f, Ui.TEXT).apply {
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.MIDDLE
        }
        row.addView(v, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        row.addView(Ui.label(this, "›", 18f, Ui.TEXT_MUTED))
        row.setOnFocusChangeListener { _, has ->
            bg.setColor(Color.parseColor(if (has) Ui.FOCUS else Ui.CARD))
            bg.setStroke(dp(if (has) 3 else 1), Color.parseColor(if (has) "#FFFFFF" else Ui.CARD_LINE))
        }
        row.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = dp(8) }
        return row
    }

    private fun hint(text: String): View =
        Ui.label(this, text, 10f, Ui.TEXT_MUTED).apply {
            setPadding(dp(2), dp(2), dp(2), dp(10))
        }

    private fun action(text: String, primary: Boolean = false, onClick: () -> Unit): TextView {
        val normal = if (primary) "#1D4ED8" else Ui.CARD
        val bg = Ui.rounded(this, normal, 8, 1, if (primary) "#7FB0FF" else Ui.CARD_LINE)
        val b = Ui.label(this, text, 14f, "#FFFFFF").apply {
            gravity = Gravity.CENTER
            setPadding(dp(18), dp(11), dp(18), dp(11))
            background = bg
            isFocusable = true
            isClickable = true
            setOnClickListener { onClick() }
        }
        b.setOnFocusChangeListener { _, has ->
            bg.setColor(Color.parseColor(if (has) Ui.FOCUS else normal))
            bg.setStroke(dp(if (has) 3 else 1), Color.parseColor(if (has) "#FFFFFF" else Ui.CARD_LINE))
        }
        b.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { marginEnd = dp(8) }
        return b
    }
}

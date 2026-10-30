package br.hy300.tv

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.TextUtils
import android.view.Gravity
import android.view.ViewGroup
import android.view.Window
import android.widget.LinearLayout
import android.widget.TextView

/** Teclado na tela: o controle remoto só tem direcional e OK, então nada de teclado do sistema. */
class KeyboardDialog(
    ctx: Context,
    private val title: String,
    initial: String,
    private val onDone: (String) -> Unit
) : Dialog(ctx) {

    private var value = initial
    private lateinit var valueView: TextView

    private val rows = listOf(
        "1234567890",
        "abcdefghij",
        "klmnopqrst",
        "uvwxyz.,:",
        "/?&=@-_#%"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window?.setLayout(dialogWidth(), ViewGroup.LayoutParams.WRAP_CONTENT)

        val ctx = context
        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            background = Ui.rounded(ctx, "#0A1526", 14, 1, Ui.CARD_LINE)
            setPadding(ctx.dp(18), ctx.dp(14), ctx.dp(18), ctx.dp(16))
        }

        root.addView(Ui.label(ctx, title, 13f, Ui.TEXT_DIM))
        valueView = Ui.label(ctx, value.ifEmpty { " " }, 16f, Ui.TEXT).apply {
            background = Ui.rounded(ctx, "#0E1626", 8, 1, Ui.CARD_LINE)
            setPadding(ctx.dp(12), ctx.dp(10), ctx.dp(12), ctx.dp(10))
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.START
        }
        root.addView(valueView, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = ctx.dp(8) })

        for (row in rows) {
            val line = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_HORIZONTAL
                setPadding(0, ctx.dp(6), 0, 0)
            }
            for (c in row) {
                line.addView(key(ctx, c.toString(), 40) { append(c.toString()) })
            }
            root.addView(line)
        }

        val actions = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(0, ctx.dp(10), 0, 0)
        }
        actions.addView(key(ctx, "Espaço", 100) { append(" ") })
        actions.addView(key(ctx, "Apagar", 90) { if (value.isNotEmpty()) value = value.dropLast(1); render() })
        actions.addView(key(ctx, "Limpar", 90) { value = ""; render() })
        actions.addView(key(ctx, "OK", 90, primary = true) { finishOk() })
        actions.addView(key(ctx, "Cancelar", 100) { dismiss() })
        root.addView(actions)

        setContentView(root)
        root.post { root.findFocus()?.requestFocus() ?: root.getChildAt(1)?.requestFocus() }
    }

    private fun dialogWidth(): Int {
        val metrics = context.resources.displayMetrics
        return (metrics.widthPixels * 0.72f).toInt()
    }

    private fun append(text: String) {
        if (value.length >= 300) return
        value += text
        render()
    }

    private fun render() {
        valueView.text = value.ifEmpty { " " }
    }

    private fun finishOk() {
        onDone(value.trim())
        dismiss()
    }

    private fun key(ctx: Context, label: String, widthDp: Int, primary: Boolean = false, onClick: () -> Unit): TextView {
        val normal = if (primary) "#1D4ED8" else Ui.CARD
        val bg = Ui.rounded(ctx, normal, 8)
        return Ui.label(ctx, label, 15f, Ui.TEXT).apply {
            gravity = Gravity.CENTER
            setPadding(ctx.dp(4), ctx.dp(9), ctx.dp(4), ctx.dp(9))
            background = bg
            isFocusable = true
            isClickable = true
            setOnClickListener { onClick() }
            setOnFocusChangeListener { v, has ->
                bg.setColor(Color.parseColor(if (has) Ui.FOCUS else normal))
                v.scaleX = if (has) 1.06f else 1f
                v.scaleY = if (has) 1.06f else 1f
            }
        }.also {
            it.layoutParams = LinearLayout.LayoutParams(ctx.dp(widthDp), ViewGroup.LayoutParams.WRAP_CONTENT)
                .apply { marginEnd = ctx.dp(4) }
        }
    }
}

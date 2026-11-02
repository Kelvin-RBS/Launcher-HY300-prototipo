package br.hy300.games

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.TextUtils
import android.view.Gravity
import android.view.ViewGroup
import android.view.Window
import android.widget.LinearLayout
import android.widget.TextView

/** Teclado na tela para o controle: o projetor não tem teclado e o direcional só move o foco. */
class NameKeyboardDialog(
    ctx: Context,
    private val title: String,
    initial: String,
    private val onDone: (String) -> Unit
) : Dialog(ctx) {

    private var value = initial
    private var upper = false
    private lateinit var valueView: TextView
    private lateinit var shiftKey: TextView
    private val letterKeys = ArrayList<Pair<TextView, Char>>()

    private val rows = listOf("1234567890", "qwertyuiop", "asdfghjkl-", "zxcvbnm._()")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window?.setLayout((context.resources.displayMetrics.widthPixels * 0.78f).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)

        val c = context
        val root = LinearLayout(c).apply {
            orientation = LinearLayout.VERTICAL
            background = box("#0A1526", "#26385A", 14)
            setPadding(c.dp(18), c.dp(14), c.dp(18), c.dp(16))
        }
        root.addView(TextView(c).apply {
            text = title
            textSize = 14f
            setTextColor(Color.parseColor("#A9B8D2"))
        })
        valueView = TextView(c).apply {
            textSize = 18f
            setTextColor(Color.WHITE)
            background = box("#0E1626", "#26385A", 8)
            setPadding(c.dp(12), c.dp(10), c.dp(12), c.dp(10))
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.START
        }
        root.addView(valueView, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = c.dp(8) })

        var firstKey: TextView? = null
        for (row in rows) {
            val line = LinearLayout(c).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_HORIZONTAL
                setPadding(0, c.dp(6), 0, 0)
            }
            for (ch in row) {
                val k = key(ch.toString(), 44, false) { append(letterOf(ch)) }
                if (ch.isLetter()) letterKeys.add(Pair(k, ch))
                if (firstKey == null) firstKey = k
                line.addView(k)
            }
            root.addView(line)
        }

        val actions = LinearLayout(c).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(0, c.dp(10), 0, 0)
        }
        shiftKey = key("Maiúsc.", 90, false) { upper = !upper; refreshLetters() }
        actions.addView(shiftKey)
        actions.addView(key("Espaço", 90, false) { append(" ") })
        actions.addView(key("Apagar", 90, false) { if (value.isNotEmpty()) value = value.dropLast(1); render() })
        actions.addView(key("Limpar", 90, false) { value = ""; render() })
        actions.addView(key("OK", 80, true) { onDone(value.trim()); dismiss() })
        actions.addView(key("Cancelar", 100, false) { dismiss() })
        root.addView(actions)

        setContentView(root)
        render()
        firstKey?.requestFocus()
    }

    private fun letterOf(ch: Char): String = if (upper && ch.isLetter()) ch.uppercaseChar().toString() else ch.toString()

    private fun refreshLetters() {
        for ((view, ch) in letterKeys) view.text = letterOf(ch)
        shiftKey.text = if (upper) "MAIÚSC." else "Maiúsc."
    }

    private fun append(text: String) {
        if (value.length >= 100) return
        value += text
        render()
    }

    private fun render() {
        valueView.text = if (value.isEmpty()) " " else value
    }

    private fun box(fill: String, line: String, radiusDp: Int): GradientDrawable = GradientDrawable().apply {
        cornerRadius = context.dp(radiusDp).toFloat()
        setColor(Color.parseColor(fill))
        setStroke(context.dp(1), Color.parseColor(line))
    }

    private fun key(label: String, widthDp: Int, primary: Boolean, onClick: () -> Unit): TextView {
        val c = context
        val normal = if (primary) "#1D4ED8" else "#16233B"
        val bg = GradientDrawable().apply {
            cornerRadius = c.dp(8).toFloat()
            setColor(Color.parseColor(normal))
            setStroke(c.dp(2), Color.parseColor("#3A4B6B"))
        }
        return TextView(c).apply {
            text = label
            textSize = 16f
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#EAF1FF"))
            setPadding(c.dp(4), c.dp(9), c.dp(4), c.dp(9))
            background = bg
            isFocusable = true
            isClickable = true
            layoutParams = LinearLayout.LayoutParams(c.dp(widthDp), ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                marginStart = c.dp(3)
                marginEnd = c.dp(3)
            }
            setOnClickListener { onClick() }
            setOnFocusChangeListener { v, has ->
                bg.setColor(Color.parseColor(if (has) "#2F7CF6" else normal))
                bg.setStroke(c.dp(if (has) 3 else 2), Color.parseColor(if (has) "#FFFFFF" else "#3A4B6B"))
                (v as TextView).setTypeface(null, if (has) Typeface.BOLD else Typeface.NORMAL)
            }
        }
    }
}

package br.hy300.games

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView

/** Componentes feitos para controle remoto e gamepad: o foco aparece com borda branca grossa, fundo azul e aumento. */
object Ui {
    private const val NORMAL_BG = "#16233B"
    private const val NORMAL_LINE = "#3A4B6B"
    private const val FOCUS_BG = "#2F7CF6"

    fun focusButton(ctx: Context, label: String, onClick: () -> Unit): TextView {
        val bg = GradientDrawable().apply {
            cornerRadius = ctx.dp(10).toFloat()
            setColor(Color.parseColor(NORMAL_BG))
            setStroke(ctx.dp(2), Color.parseColor(NORMAL_LINE))
        }
        val button = TextView(ctx).apply {
            text = label
            textSize = 16f
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#C5D2EA"))
            setPadding(ctx.dp(14), ctx.dp(12), ctx.dp(14), ctx.dp(12))
            background = bg
            isFocusable = true
            isClickable = true
            setOnClickListener { onClick() }
        }
        button.setOnFocusChangeListener { v, has ->
            bg.setColor(Color.parseColor(if (has) FOCUS_BG else NORMAL_BG))
            bg.setStroke(ctx.dp(if (has) 4 else 2), Color.parseColor(if (has) "#FFFFFF" else NORMAL_LINE))
            (v as TextView).setTextColor(if (has) Color.WHITE else Color.parseColor("#C5D2EA"))
            v.setTypeface(null, if (has) Typeface.BOLD else Typeface.NORMAL)
            v.animate().scaleX(if (has) 1.06f else 1f).scaleY(if (has) 1.06f else 1f).setDuration(100).start()
        }
        return button
    }

    /** Linha de ajuste: esquerda e direita trocam o valor; OK avança para o próximo. */
    fun optionRow(ctx: Context, title: String, values: List<String>, get: () -> Int, set: (Int) -> Unit): View {
        val bg = GradientDrawable().apply {
            cornerRadius = ctx.dp(10).toFloat()
            setColor(Color.parseColor("#0E1626"))
            setStroke(ctx.dp(2), Color.parseColor("#1E2C45"))
        }
        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(ctx.dp(16), 0, ctx.dp(16), 0)
            background = bg
            isFocusable = true
            isClickable = true
        }
        val name = TextView(ctx).apply {
            text = title
            textSize = 17f
            setTextColor(Color.WHITE)
        }
        val value = TextView(ctx).apply {
            textSize = 17f
            setTextColor(Color.parseColor("#C5D2EA"))
        }
        row.addView(name, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        row.addView(value)

        fun refresh() {
            value.text = "\u2039  " + values[get()] + "  \u203A"
        }
        fun change(delta: Int) {
            set((get() + delta + values.size) % values.size)
            refresh()
        }
        refresh()

        row.setOnClickListener { change(1) }
        row.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                change(-1)
                true
            } else if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                change(1)
                true
            } else {
                false
            }
        }
        row.setOnFocusChangeListener { _, has ->
            bg.setColor(Color.parseColor(if (has) FOCUS_BG else "#0E1626"))
            bg.setStroke(ctx.dp(if (has) 4 else 2), Color.parseColor(if (has) "#FFFFFF" else "#1E2C45"))
            value.setTextColor(if (has) Color.WHITE else Color.parseColor("#C5D2EA"))
        }
        return row
    }
}

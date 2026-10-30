package br.hy300.tv

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.LinearLayout
import android.widget.TextView

/** Peças de interface no mesmo visual do launcher: azul-escuro, borda branca no foco, texto claro. */
object Ui {
    const val BG_DARK = "#04080F"
    const val CARD = "#16233B"
    const val CARD_LINE = "#2A3A57"
    const val FOCUS = "#2F7CF6"
    const val TEXT = "#EAF1FF"
    const val TEXT_DIM = "#A9B8D2"
    const val TEXT_MUTED = "#7E8CA6"
    const val LIVE = "#FF4D4D"

    fun rounded(ctx: Context, color: String, radiusDp: Int, strokeDp: Int = 0, strokeColor: String = CARD_LINE) =
        GradientDrawable().apply {
            cornerRadius = ctx.dp(radiusDp).toFloat()
            setColor(Color.parseColor(color))
            if (strokeDp > 0) setStroke(ctx.dp(strokeDp), Color.parseColor(strokeColor))
        }

    fun label(ctx: Context, text: String, size: Float, color: String = TEXT): TextView =
        TextView(ctx).apply {
            this.text = text
            textSize = size
            setTextColor(Color.parseColor(color))
        }

    /** Selo "AO VIVO" com bolinha. */
    fun liveBadge(ctx: Context): LinearLayout {
        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = rounded(ctx, "#2A1414", 4)
            setPadding(ctx.dp(6), ctx.dp(2), ctx.dp(6), ctx.dp(2))
        }
        row.addView(View(ctx).apply { background = rounded(ctx, LIVE, 20) },
            LinearLayout.LayoutParams(ctx.dp(6), ctx.dp(6)).apply { marginEnd = ctx.dp(4) })
        row.addView(label(ctx, "AO VIVO", 8f, LIVE))
        return row
    }
}

/** Lista de categorias (Todos, Favoritos, grupos da lista). */
class GroupAdapter(
    private val ctx: Context,
    private val items: List<String>
) : BaseAdapter() {

    override fun getCount() = items.size
    override fun getItem(position: Int) = items[position]
    override fun getItemId(position: Int) = position.toLong()

    override fun getView(position: Int, convert: View?, parent: ViewGroup?): View {
        val row = (convert as? LinearLayout) ?: LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(ctx.dp(12), 0, ctx.dp(8), 0)
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ctx.dp(30))
        }
        row.removeAllViews()
        val name = items[position]
        val color = when (name) {
            TvHomeActivity.GROUP_FAV -> "#FFD166"
            TvHomeActivity.GROUP_SOURCE -> "#7FB0FF"
            else -> Ui.TEXT_DIM
        }
        row.addView(Ui.label(ctx, name, 13f, color),
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        return row
    }
}

/** Lista de canais: número, nome e grupo. */
class ChannelAdapter(
    private val ctx: Context,
    private val items: List<Channel>,
    private val favorites: Set<String>,
    private val currentKey: String?
) : BaseAdapter() {

    override fun getCount() = items.size
    override fun getItem(position: Int) = items[position]
    override fun getItemId(position: Int) = items[position].number.toLong()

    override fun getView(position: Int, convert: View?, parent: ViewGroup?): View {
        val ch = items[position]
        val row = (convert as? LinearLayout) ?: LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(ctx.dp(10), 0, ctx.dp(10), 0)
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ctx.dp(32))
        }
        row.removeAllViews()

        val playing = currentKey != null && currentKey == ch.name + "|" + ch.url
        if (playing) {
            row.addView(View(ctx).apply { background = Ui.rounded(ctx, Ui.LIVE, 20) },
                LinearLayout.LayoutParams(ctx.dp(5), ctx.dp(5)).apply { marginEnd = ctx.dp(6) })
        }

        row.addView(Ui.label(ctx, ch.number.toString(), 12f, Ui.TEXT_MUTED).apply {
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
        }, LinearLayout.LayoutParams(ctx.dp(30), ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            marginEnd = ctx.dp(8)
        })

        row.addView(Ui.label(ctx, ch.name, 14f, if (playing) "#FFFFFF" else Ui.TEXT).apply {
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            typeface = if (playing) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

        if (Favorites.isFavorite(favorites, ch)) {
            row.addView(Ui.label(ctx, "\u2605", 13f, "#FFD166"),
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    marginStart = ctx.dp(6)
                })
        }
        return row
    }
}

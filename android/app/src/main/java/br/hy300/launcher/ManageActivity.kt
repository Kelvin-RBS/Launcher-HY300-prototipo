package br.hy300.launcher

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast

/** Escolha dos até 5 atalhos de apps da tela inicial. OK adiciona ou remove; Voltar conclui. */
class ManageActivity : Activity() {

    private val pinned = ArrayList<String>()
    private lateinit var apps: List<AppEntry>
    private lateinit var adapter: BaseAdapter
    private lateinit var counter: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        apps = AppRepo.installed(this)
        pinned.addAll(AppRepo.pinned(this))

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#060A14"))
            setPadding(dp(43), dp(24), dp(43), dp(16))
        }
        root.addView(TextView(this).apply {
            text = "Gerenciar atalhos"
            textSize = 28f
            setTextColor(Color.parseColor("#EAF1FF"))
        })
        counter = TextView(this).apply {
            textSize = 14f
            setTextColor(Color.parseColor("#A9B8D2"))
            setPadding(0, dp(4), 0, dp(12))
        }
        root.addView(counter)

        val list = ListView(this).apply {
            divider = null
            dividerHeight = 0
            selector = GradientDrawable().apply {
                cornerRadius = dp(8).toFloat()
                setColor(Color.argb(40, 227, 238, 255))
                setStroke(dp(2), Color.rgb(227, 238, 255))
            }
        }
        adapter = object : BaseAdapter() {
            override fun getCount() = apps.size
            override fun getItem(position: Int) = apps[position]
            override fun getItemId(position: Int) = position.toLong()
            override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
                val app = apps[position]
                val row = LinearLayout(this@ManageActivity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(dp(10), dp(6), dp(10), dp(6))
                }
                row.addView(ImageView(this@ManageActivity).apply {
                    setImageDrawable(app.icon)
                }, LinearLayout.LayoutParams(dp(36), dp(36)))
                row.addView(TextView(this@ManageActivity).apply {
                    text = app.label
                    textSize = 17f
                    setTextColor(Color.WHITE)
                    setPadding(dp(12), 0, 0, 0)
                }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
                row.addView(TextView(this@ManageActivity).apply {
                    text = if (pinned.contains(app.pkg)) "\u2713" else ""
                    textSize = 20f
                    setTextColor(Color.rgb(77, 163, 255))
                })
                return row
            }
        }
        list.adapter = adapter
        list.setOnItemClickListener { _, _, position, _ -> toggle(apps[position].pkg) }
        root.addView(list, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        setContentView(root)
        refreshCounter()
        list.requestFocus()
    }

    private fun toggle(pkg: String) {
        if (pinned.contains(pkg)) {
            pinned.remove(pkg)
        } else if (pinned.size >= AppRepo.MAX_PINNED) {
            Toast.makeText(this, "Limite de " + AppRepo.MAX_PINNED + " atalhos. Remova um antes.", Toast.LENGTH_SHORT).show()
            return
        } else {
            pinned.add(pkg)
        }
        AppRepo.savePinned(this, pinned)
        adapter.notifyDataSetChanged()
        refreshCounter()
    }

    private fun refreshCounter() {
        counter.text = pinned.size.toString() + " de " + AppRepo.MAX_PINNED + " atalhos  \u00B7  OK adiciona ou remove  \u00B7  Voltar conclui"
    }
}

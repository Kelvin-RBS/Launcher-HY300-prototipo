package br.hy300.launcher

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.GridView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

/**
 * Todos os apps. OK abre o app; segurar OK por 2 segundos pede para desinstalar (o Android mostra
 * "Desinstalar" e "Cancelar"). O primeiro item abre a loja para instalar apps.
 */
class AppsActivity : Activity() {

    private val handler = Handler(Looper.getMainLooper())
    private var apps: List<AppEntry> = emptyList()
    private lateinit var grid: GridView
    private lateinit var adapter: BaseAdapter
    private var holdRunnable: Runnable? = null
    private var holdFired = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#060A14"))
            setPadding(dp(43), dp(24), dp(43), dp(16))
        }
        root.addView(TextView(this).apply {
            text = "Aplicativos"
            textSize = 28f
            setTextColor(Color.parseColor("#EAF1FF"))
        })
        root.addView(TextView(this).apply {
            text = "OK abre  \u00B7  segure OK por 2 segundos para desinstalar  \u00B7  Voltar sai"
            textSize = 13f
            setTextColor(Color.parseColor("#A9B8D2"))
            setPadding(0, dp(4), 0, dp(12))
        })

        grid = GridView(this).apply {
            numColumns = 6
            stretchMode = GridView.STRETCH_COLUMN_WIDTH
            verticalSpacing = dp(8)
            horizontalSpacing = dp(8)
            isVerticalScrollBarEnabled = false
            clipToPadding = false
            setPadding(dp(4), dp(4), dp(4), dp(4))
            selector = GradientDrawable().apply {
                cornerRadius = dp(10).toFloat()
                setColor(Color.argb(40, 227, 238, 255))
                setStroke(dp(2), Color.rgb(227, 238, 255))
            }
            setDrawSelectorOnTop(true)
        }
        adapter = object : BaseAdapter() {
            // Posição 0 é o botão "Instalar apps"; depois vêm os apps
            override fun getCount() = apps.size + 1
            override fun getItem(position: Int): Any = position
            override fun getItemId(position: Int) = position.toLong()
            override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
                val cell = LinearLayout(this@AppsActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER_HORIZONTAL
                    setPadding(dp(4), dp(10), dp(4), dp(8))
                }
                val icon = ImageView(this@AppsActivity)
                val label = TextView(this@AppsActivity).apply {
                    gravity = Gravity.CENTER
                    maxLines = 2
                    textSize = 11f
                    setTextColor(Color.parseColor("#C5D2EA"))
                }
                if (position == 0) {
                    icon.setImageResource(R.drawable.ic_plus)
                    icon.setPadding(dp(12), dp(12), dp(12), dp(12))
                    icon.background = GradientDrawable().apply {
                        cornerRadius = dp(10).toFloat()
                        setColor(Color.parseColor("#2F5DA8"))
                    }
                    label.text = "Instalar apps"
                } else {
                    val app = apps[position - 1]
                    icon.setImageDrawable(app.icon)
                    label.text = app.label
                }
                cell.addView(icon, LinearLayout.LayoutParams(dp(48), dp(48)))
                cell.addView(label, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    topMargin = dp(6)
                })
                return cell
            }
        }
        grid.adapter = adapter
        grid.setOnItemClickListener { _, _, position, _ -> onSelect(position) }
        // OK curto abre; OK segurado desinstala
        grid.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER ||
                keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER
            ) {
                handleOk(event)
                true
            } else {
                false
            }
        }
        root.addView(grid, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        setContentView(root)
    }

    override fun onResume() {
        super.onResume()
        apps = AppRepo.installed(this, true)
        adapter.notifyDataSetChanged()
        grid.requestFocus()
        if (grid.selectedItemPosition < 0) grid.setSelection(0)
    }

    private fun handleOk(event: KeyEvent) {
        val position = grid.selectedItemPosition
        if (event.action == KeyEvent.ACTION_DOWN) {
            if (event.repeatCount == 0 && position >= 0) {
                holdFired = false
                val r = Runnable {
                    holdFired = true
                    onHold(position)
                }
                holdRunnable = r
                handler.postDelayed(r, 2000)
            }
        } else if (event.action == KeyEvent.ACTION_UP) {
            holdRunnable?.let { handler.removeCallbacks(it) }
            holdRunnable = null
            if (!holdFired && position >= 0) onSelect(position)
            holdFired = false
        }
    }

    private fun onSelect(position: Int) {
        if (position == 0) {
            installApps()
            return
        }
        val app = apps[position - 1]
        val intent = AppRepo.launchIntent(packageManager, app.pkg)
        if (intent == null) {
            Toast.makeText(this, "Não foi possível abrir", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Não foi possível abrir", Toast.LENGTH_SHORT).show()
        }
    }

    private fun onHold(position: Int) {
        if (position == 0) return
        val app = apps[position - 1]
        if (app.system) {
            Toast.makeText(this, app.label + " é do sistema e não pode ser desinstalado", Toast.LENGTH_LONG).show()
            return
        }
        // O Android pergunta "Desinstalar" ou "Cancelar"
        try {
            startActivity(Intent(Intent.ACTION_DELETE, Uri.parse("package:" + app.pkg)))
        } catch (e: Exception) {
            Toast.makeText(this, "Não foi possível desinstalar", Toast.LENGTH_SHORT).show()
        }
    }

    private fun installApps() {
        val stores = listOf("Google Play" to "com.android.vending", "Aptoide TV" to "cm.aptoidetv.pt")
            .filter { AppRepo.launchIntent(packageManager, it.second) != null }
        when (stores.size) {
            0 -> Toast.makeText(this, "Nenhuma loja de apps instalada", Toast.LENGTH_LONG).show()
            1 -> openStore(stores[0].second)
            else -> AlertDialog.Builder(this)
                .setTitle("Instalar aplicativos")
                .setItems(stores.map { it.first }.toTypedArray()) { _, which -> openStore(stores[which].second) }
                .show()
        }
    }

    private fun openStore(pkg: String) {
        val intent = AppRepo.launchIntent(packageManager, pkg) ?: return
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Não foi possível abrir a loja", Toast.LENGTH_SHORT).show()
        }
    }
}

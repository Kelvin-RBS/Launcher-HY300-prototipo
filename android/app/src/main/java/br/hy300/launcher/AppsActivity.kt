package br.hy300.launcher

import android.app.Activity
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
 * Todos os apps (Configurações > Aplicativos). OK abre o app; segurar OK por 2 segundos pede para
 * desinstalar (o Android mostra "Desinstalar" e "Cancelar").
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
            override fun getCount() = apps.size
            override fun getItem(position: Int): Any = position
            override fun getItemId(position: Int) = position.toLong()
            override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
                val app = apps[position]
                val cell = LinearLayout(this@AppsActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER_HORIZONTAL
                    setPadding(dp(4), dp(10), dp(4), dp(8))
                }
                val icon = ImageView(this@AppsActivity).apply { setImageDrawable(app.icon) }
                val label = TextView(this@AppsActivity).apply {
                    text = app.label
                    gravity = Gravity.CENTER
                    maxLines = 2
                    textSize = 11f
                    setTextColor(Color.parseColor("#C5D2EA"))
                }
                cell.addView(icon, LinearLayout.LayoutParams(dp(48), dp(48)))
                cell.addView(label, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    topMargin = dp(6)
                })
                return cell
            }
        }
        grid.adapter = adapter
        grid.setOnItemClickListener { _, _, position, _ -> openApp(position) }
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
        if (grid.selectedItemPosition < 0 && apps.isNotEmpty()) grid.setSelection(0)
    }

    private fun handleOk(event: KeyEvent) {
        val position = grid.selectedItemPosition
        if (event.action == KeyEvent.ACTION_DOWN) {
            if (event.repeatCount == 0 && position >= 0) {
                holdFired = false
                val r = Runnable {
                    holdFired = true
                    uninstall(position)
                }
                holdRunnable = r
                handler.postDelayed(r, 2000)
            }
        } else if (event.action == KeyEvent.ACTION_UP) {
            holdRunnable?.let { handler.removeCallbacks(it) }
            holdRunnable = null
            if (!holdFired && position >= 0) openApp(position)
            holdFired = false
        }
    }

    private fun openApp(position: Int) {
        if (position < 0 || position >= apps.size) return
        val app = apps[position]
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

    private fun uninstall(position: Int) {
        if (position < 0 || position >= apps.size) return
        val app = apps[position]
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
}

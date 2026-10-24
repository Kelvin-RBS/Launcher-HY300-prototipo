package br.hy300.launcher

import android.app.Activity
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextClock
import android.widget.TextView
import android.widget.Toast

/** Tela inicial: topo (rede, bluetooth, configurações, hora), título do item em foco e fileira de atalhos. */
class HomeActivity : Activity() {

    private val settingsPkg = "com.htc.htcsettings"
    private val settingsPrefix = "com.htc.activity."

    private lateinit var titleView: TextView
    private lateinit var descView: TextView
    private lateinit var track: LinearLayout
    private var firstCard: View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildUi())
    }

    override fun onResume() {
        super.onResume()
        hideSystemBars()
        rebuildRow()
    }

    // Um launcher não deve fechar com o botão Voltar
    @Deprecated("Launcher: ignora o botão Voltar")
    override fun onBackPressed() {
    }

    @Suppress("DEPRECATION")
    private fun hideSystemBars() {
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
    }

    // ---------------------------------------------------------------- interface

    private fun buildUi(): View {
        val mp = ViewGroup.LayoutParams.MATCH_PARENT
        val wc = ViewGroup.LayoutParams.WRAP_CONTENT
        val root = FrameLayout(this)
        root.setBackgroundColor(Color.BLACK)

        // Fundo e degradês para o texto ficar legível
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

        // Topo: rede, bluetooth, engrenagem e hora
        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        top.addView(roundButton(R.drawable.ic_wifi, "Rede", "Wi-Fi e conexão com a internet.") {
            openSettings("WIFIActivity")
        })
        top.addView(roundButton(R.drawable.ic_bluetooth, "Bluetooth", "Fones, caixas de som e controles.") {
            openSettings("BluetoothSettingActivity")
        })
        top.addView(roundButton(R.drawable.ic_gear, "Configurações", "Projeção, rede, áudio, data e hora e mais.") {
            showSettingsMenu()
        })
        top.addView(TextClock(this).apply {
            setTextColor(Color.WHITE)
            textSize = 17f
            setPadding(dp(6), 0, 0, 0)
        })
        root.addView(top, FrameLayout.LayoutParams(wc, wc, Gravity.TOP or Gravity.END).apply {
            topMargin = dp(16)
            marginEnd = dp(32)
        })

        // Título e descrição do item em foco
        val info = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        info.addView(TextView(this).apply {
            text = "MEU PROJETOR"
            setTextColor(Color.parseColor("#A9B8D2"))
            textSize = 11f
            letterSpacing = 0.24f
        })
        titleView = TextView(this).apply {
            setTextColor(Color.parseColor("#EAF1FF"))
            textSize = 46f
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            setPadding(0, dp(6), 0, dp(6))
        }
        descView = TextView(this).apply {
            setTextColor(Color.parseColor("#D5DFF2"))
            textSize = 18f
            maxLines = 2
        }
        info.addView(titleView)
        info.addView(descView)
        root.addView(info, FrameLayout.LayoutParams(dp(470), wc).apply {
            leftMargin = dp(43)
            topMargin = dp(100)
        })

        // Fileira de atalhos
        root.addView(TextView(this).apply {
            text = "Atalhos"
            setTextColor(Color.parseColor("#EAF1FF"))
            textSize = 13f
        }, FrameLayout.LayoutParams(wc, wc).apply {
            leftMargin = dp(43)
            topMargin = dp(310)
        })
        track = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            clipChildren = false
        }
        val scroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
            clipToPadding = false
            clipChildren = false
            setPadding(dp(10), dp(10), dp(10), dp(10))
            addView(track)
        }
        root.addView(scroll, FrameLayout.LayoutParams(mp, wc).apply {
            leftMargin = dp(33)
            rightMargin = dp(33)
            topMargin = dp(336)
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
            focusFx(view, bgd, has, 1.12f, 2)
            if (has) show(name, text)
        }
        v.layoutParams = LinearLayout.LayoutParams(dp(34), dp(34)).apply { marginEnd = dp(10) }
        return v
    }

    private fun focusFx(v: View, stroke: GradientDrawable, has: Boolean, scale: Float, strokeDp: Int) {
        val s = if (has) scale else 1f
        v.animate().scaleX(s).scaleY(s).setDuration(120).start()
        stroke.setStroke(if (has) dp(strokeDp) else 0, Color.rgb(227, 238, 255))
    }

    private fun show(title: String, desc: String) {
        titleView.text = title
        descView.text = desc
    }

    // ---------------------------------------------------------------- fileira de atalhos

    private fun rebuildRow() {
        track.removeAllViews()
        firstCard = null
        val byPkg = AppRepo.installed(this).associateBy { it.pkg }
        var count = 0
        for (pkg in AppRepo.pinned(this)) {
            val app = byPkg[pkg] ?: continue
            if (count >= AppRepo.MAX_PINNED) break
            addApp(app)
            count++
        }
        // Atalhos fixos: não podem ser removidos nem movidos
        addFixed("Jogos", "Jogos retrô de 16 bits. Em breve, com RetroArch.", "#6A4FB3", R.drawable.ic_gamepad, 6) {
            toast("Em breve: jogos de 16 bits com RetroArch")
        }
        addFixed("Arquivos", "Armazenamento interno e pendrive.", "#8A6A2F", R.drawable.ic_folder, 20) {
            launch("com.softwinner.TvdFileManager")
        }
        addFixed("HDMI", "Entrada externa: videogame, notebook ou TV box.", "#2F5DA8", R.drawable.ic_hdmi, 20) {
            openComponent("com.softwinner.awlivetv", "com.softwinner.awlivetv.MainActivity")
        }
        addFixed("Gerenciar atalhos", "Adicione ou remova os atalhos da tela inicial.", "#4A5568", R.drawable.ic_sliders, 20) {
            startActivity(Intent(this, ManageActivity::class.java))
        }
        firstCard?.requestFocus()
    }

    private fun addApp(a: AppEntry) {
        val useBanner = AppRepo.PREFER_BANNER && a.banner != null
        val content = ImageView(this)
        if (useBanner) {
            content.setImageDrawable(a.banner)
            content.scaleType = ImageView.ScaleType.CENTER_CROP
        } else {
            content.setImageDrawable(a.icon)
            content.scaleType = ImageView.ScaleType.FIT_CENTER
            content.setPadding(dp(14), dp(10), dp(14), dp(10))
        }
        val color = if (useBanner) Color.BLACK else Color.parseColor("#16233B")
        addTile(a.label, "Abrir " + a.label + ".", color, content) { launch(a.pkg) }
    }

    private fun addFixed(label: String, descr: String, colorHex: String, icon: Int, padDp: Int, onClick: () -> Unit) {
        val content = ImageView(this).apply {
            setImageResource(icon)
            scaleType = ImageView.ScaleType.FIT_CENTER
            setPadding(dp(padDp), dp(padDp), dp(padDp), dp(padDp))
        }
        addTile(label, descr, Color.parseColor(colorHex), content, onClick)
    }

    private fun addTile(label: String, descr: String, color: Int, content: View, onClick: () -> Unit) {
        val back = GradientDrawable().apply {
            cornerRadius = dp(9).toFloat()
            setColor(color)
        }
        // A borda de foco fica na frente da imagem do app
        val border = GradientDrawable().apply {
            cornerRadius = dp(9).toFloat()
            setColor(Color.TRANSPARENT)
        }
        val card = FrameLayout(this).apply {
            background = back
            foreground = border
            outlineProvider = ViewOutlineProvider.BACKGROUND
            clipToOutline = true
            isFocusable = true
            isClickable = true
            setOnClickListener { onClick() }
            addView(content, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        }
        card.setOnFocusChangeListener { v, has ->
            focusFx(v, border, has, 1.1f, 3)
            if (has) show(label, descr)
        }
        val item = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
        }
        item.addView(card, LinearLayout.LayoutParams(dp(112), dp(67)))
        item.addView(TextView(this).apply {
            text = label
            gravity = Gravity.CENTER
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            textSize = 11f
            setTextColor(Color.parseColor("#C5D2EA"))
        }, LinearLayout.LayoutParams(dp(112), ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(8) })
        track.addView(item, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            marginEnd = dp(12)
        })
        if (firstCard == null) firstCard = card
    }

    // ---------------------------------------------------------------- ações

    private fun showSettingsMenu() {
        val items = listOf(
            "Rede" to "WIFIActivity",
            "Bluetooth" to "BluetoothSettingActivity",
            "Idioma" to "LanguageActivity",
            "Projeção (zoom, método, keystone)" to "ProjectionActivity",
            "Brilho e cor" to "PictureModeActivity",
            "Saída de áudio" to "SoundChannelActivity",
            "Data e hora" to "DateTimeActivity",
            "Fuso horário" to "TimeZoneActivity",
            "Sobre o dispositivo" to "AboutDeviceActivity",
            "Atualização local (pendrive)" to "SystemUpdateActivity",
            "Atualização online" to "UpdateActivity"
        )
        AlertDialog.Builder(this)
            .setTitle("Configurações")
            .setItems(items.map { it.first }.toTypedArray()) { _, which ->
                openSettings(items[which].second)
            }
            .show()
    }

    private fun openSettings(activityName: String) = openComponent(settingsPkg, settingsPrefix + activityName)

    private fun openComponent(pkg: String, cls: String) {
        try {
            startActivity(Intent().setComponent(ComponentName(pkg, cls)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: Exception) {
            toast("Não foi possível abrir")
        }
    }

    private fun launch(pkg: String) {
        val intent = AppRepo.launchIntent(packageManager, pkg)
        if (intent == null) {
            toast("Não foi possível abrir")
            return
        }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            toast("Não foi possível abrir")
        }
    }

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}

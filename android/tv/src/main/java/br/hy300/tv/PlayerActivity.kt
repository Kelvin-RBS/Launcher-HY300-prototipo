package br.hy300.tv

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.text.TextUtils
import android.view.Gravity
import android.view.KeyEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextClock
import android.widget.TextView
import android.widget.Toast

/**
 * Player ao vivo: vídeo em tela cheia e, ao apertar OK, a lista de canais por cima —
 * do mesmo jeito que os apps de TV do aparelho fazem.
 */
class PlayerActivity : Activity(), SurfaceHolder.Callback {

    companion object {
        const val EXTRA_INDEX = "index"

        private const val ASPECT_FILL = 0
        private const val ASPECT_FIT = 1
        private const val ASPECT_16_9 = 2
        private const val ASPECT_4_3 = 3
        private val ASPECT_NAMES = arrayOf("Preencher", "Ajustar", "16:9", "4:3")
    }

    private lateinit var surfaceView: SurfaceView
    private lateinit var surfaceBox: FrameLayout
    private lateinit var infobar: LinearLayout
    private lateinit var infoNumber: TextView
    private lateinit var infoName: TextView
    private lateinit var infoSub: TextView
    private lateinit var topBar: LinearLayout
    private lateinit var panel: LinearLayout
    private lateinit var panelList: ListView
    private lateinit var aspectRow: TextView
    private lateinit var favRow: TextView
    private lateinit var loading: TextView
    private lateinit var errorBox: LinearLayout
    private lateinit var errorText: TextView
    private lateinit var numberBuffer: TextView

    private var player: MediaPlayer? = null
    private var holder: SurfaceHolder? = null
    private var surfaceReady = false
    private var pendingIndex = -1
    private var currentIndex = 0
    private var videoW = 0
    private var videoH = 0
    private var aspect = ASPECT_FILL
    private var retries = 0
    private var panelShown: List<Channel> = emptyList()
    private var digitBuffer = ""
    private var panelVisible = false

    private val ui = Handler(Looper.getMainLooper())
    private val hideBar = Runnable { hideChrome() }
    private val digitTimeout = Runnable { commitDigits() }
    private val retryRunnable = Runnable { tune(currentIndex) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(buildUi())
        surfaceView.holder.addCallback(this)
        currentIndex = intent.getIntExtra(EXTRA_INDEX, 0).coerceAtLeast(0)
        pendingIndex = currentIndex
    }

    override fun onDestroy() {
        ui.removeCallbacksAndMessages(null)
        releasePlayer()
        super.onDestroy()
    }

    override fun onPause() {
        super.onPause()
        releasePlayer()
    }

    override fun onResume() {
        super.onResume()
        if (!surfaceReady) return
        if (player == null && Library.channels.isNotEmpty()) tune(currentIndex)
    }

    // ------------------------------------------------------------------ interface

    private fun buildUi(): View {
        val mp = ViewGroup.LayoutParams.MATCH_PARENT
        val wc = ViewGroup.LayoutParams.WRAP_CONTENT
        val root = FrameLayout(this).apply { setBackgroundColor(Color.BLACK) }

        surfaceBox = FrameLayout(this).apply { setBackgroundColor(Color.BLACK) }
        surfaceView = SurfaceView(this)
        surfaceBox.addView(surfaceView, FrameLayout.LayoutParams(mp, mp))
        root.addView(surfaceBox, FrameLayout.LayoutParams(mp, mp))

        // Topo com hora e selo AO VIVO
        topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(24), dp(14), dp(24), 0)
        }
        topBar.addView(Ui.liveBadge(this))
        topBar.addView(TextClock(this).apply {
            setTextColor(Color.WHITE)
            textSize = 14f
            setPadding(dp(14), 0, 0, 0)
        })
        root.addView(topBar, FrameLayout.LayoutParams(mp, wc, Gravity.TOP))

        // Barra de informações do canal
        infobar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#E60C0D0F"))
            }
            setPadding(dp(22), dp(12), dp(22), dp(12))
            visibility = View.GONE
        }
        val numbers = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
        }
        infoNumber = Ui.label(this, "", 30f, "#FFFFFF").apply { includeFontPadding = false }
        infoName = Ui.label(this, "", 19f, "#FFFFFF").apply {
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.MARQUEE
            marqueeRepeatLimit = -1
            isSingleLine = true
        }
        infoSub = Ui.label(this, "", 11f, "#BFBFBF").apply { maxLines = 1 }
        numbers.addView(infoNumber)
        numbers.addView(infoName)
        numbers.addView(infoSub)
        infobar.addView(numbers, LinearLayout.LayoutParams(0, wc, 1f))

        val hints = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.END
        }
        hints.addView(Ui.label(this, "OK lista  ·  ▲▼ canal", 10f, "#BFBFBF"))
        hints.addView(Ui.label(this, "Imagem e favorito na lista (OK)", 10f, "#8C9194"))
        infobar.addView(hints, LinearLayout.LayoutParams(wc, wc))
        root.addView(infobar, FrameLayout.LayoutParams(mp, wc, Gravity.BOTTOM))

        // Painel de canais (aparece com OK)
        panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#F20A0F18"))
            setPadding(dp(6), dp(10), dp(6), dp(10))
            visibility = View.GONE
        }
        panel.addView(Ui.label(this, "CANAIS", 11f, Ui.TEXT_MUTED).apply {
            setPadding(dp(10), 0, 0, dp(6))
            letterSpacing = 0.2f
        })
        // Linha de imagem: OK aqui troca entre preencher/ajustar/16:9/4:3.
        aspectRow = Ui.label(this, "", 12f, Ui.TEXT).apply {
            background = Ui.rounded(this@PlayerActivity, Ui.CARD, 6, 1, Ui.CARD_LINE)
            setPadding(dp(10), dp(8), dp(10), dp(8))
            isFocusable = true
            isClickable = true
            setOnClickListener { cycleAspect() }
        }
        panel.addView(aspectRow, LinearLayout.LayoutParams(mp, wc).apply { bottomMargin = dp(6) })
        favRow = Ui.label(this, "", 12f, Ui.TEXT).apply {
            background = Ui.rounded(this@PlayerActivity, Ui.CARD, 6, 1, Ui.CARD_LINE)
            setPadding(dp(10), dp(8), dp(10), dp(8))
            isFocusable = true
            isClickable = true
            setOnClickListener { toggleFavorite() }
        }
        panel.addView(favRow, LinearLayout.LayoutParams(mp, wc).apply { bottomMargin = dp(6) })
        panelList = ListView(this).apply {
            divider = null
            dividerHeight = 0
            selector = Ui.rounded(this@PlayerActivity, Ui.FOCUS, 6)
            isFocusable = true
        }
        panel.addView(panelList, LinearLayout.LayoutParams(mp, 0, 1f))
        panel.addView(Ui.label(this, "OK assistir  ·  ◀ fecha  ·  ▲▼ navega", 9f, Ui.TEXT_MUTED)
            .apply { setPadding(dp(10), dp(6), dp(0), 0) })
        root.addView(panel, FrameLayout.LayoutParams(dp(300), mp, Gravity.START))

        // Sintonizando
        loading = Ui.label(this, "Sintonizando canal…", 15f, "#FFFFFF").apply {
            gravity = Gravity.CENTER
            background = Ui.rounded(this@PlayerActivity, "#99040A14", 8)
            setPadding(dp(20), dp(12), dp(20), dp(12))
            visibility = View.GONE
        }
        root.addView(loading, FrameLayout.LayoutParams(wc, wc, Gravity.CENTER))

        // Erro
        errorBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = Ui.rounded(this@PlayerActivity, "#E60C0D0F", 10, 1, Ui.CARD_LINE)
            setPadding(dp(24), dp(20), dp(24), dp(20))
            visibility = View.GONE
        }
        errorText = Ui.label(this, "", 13f, "#FFFFFF").apply {
            gravity = Gravity.CENTER
            maxLines = 3
        }
        errorBox.addView(errorText, LinearLayout.LayoutParams(dp(360), wc))
        errorBox.addView(retryButton(), LinearLayout.LayoutParams(dp(200), wc).apply {
            gravity = Gravity.CENTER_HORIZONTAL
            topMargin = dp(14)
        })
        root.addView(errorBox, FrameLayout.LayoutParams(wc, wc, Gravity.CENTER))

        // Número digitado
        numberBuffer = Ui.label(this, "", 44f, "#FFFFFF").apply {
            gravity = Gravity.CENTER
            background = Ui.rounded(this@PlayerActivity, "#CC040A14", 10)
            setPadding(dp(24), dp(8), dp(24), dp(8))
            visibility = View.GONE
        }
        root.addView(numberBuffer, FrameLayout.LayoutParams(wc, wc, Gravity.TOP or Gravity.CENTER_HORIZONTAL).apply {
            topMargin = dp(70)
        })

        return root
    }

    private fun retryButton(): TextView {
        val bg = Ui.rounded(this, Ui.CARD, 8, 2, "#3A4B6B")
        val b = Ui.label(this, "Tentar de novo", 14f, Ui.TEXT_DIM)
        b.apply {
            gravity = Gravity.CENTER
            setPadding(dp(16), dp(10), dp(16), dp(10))
            background = bg
            isFocusable = true
            isClickable = true
            setOnClickListener {
                retries = 0
                hideError()
                tune(currentIndex)
            }
        }
        b.setOnFocusChangeListener { _, has ->
            bg.setColor(Color.parseColor(if (has) Ui.FOCUS else Ui.CARD))
            bg.setStroke(dp(if (has) 3 else 2), Color.parseColor(if (has) "#FFFFFF" else "#3A4B6B"))
            b.setTextColor(Color.parseColor(if (has) "#FFFFFF" else Ui.TEXT_DIM))
        }
        return b
    }

    // ------------------------------------------------------------------ vídeo

    override fun surfaceCreated(h: SurfaceHolder) {
        holder = h
        surfaceReady = true
        h.setKeepScreenOn(true)
        if (pendingIndex >= 0) {
            tune(pendingIndex)
            pendingIndex = -1
        }
    }

    override fun surfaceChanged(h: SurfaceHolder, format: Int, width: Int, height: Int) {
        applyAspect()
    }

    override fun surfaceDestroyed(h: SurfaceHolder) {
        surfaceReady = false
        holder = null
        releasePlayer()
    }

    private fun tune(index: Int) {
        val list = Library.channels
        if (list.isEmpty()) {
            finish()
            return
        }
        ui.removeCallbacks(retryRunnable)
        val i = ((index % list.size) + list.size) % list.size
        currentIndex = i
        val ch = list[i]
        LastChannel.set(this, ch)
        updateInfo(ch)
        updatePanelRows()
        hideError()
        showLoading(true)
        releasePlayer()
        if (!surfaceReady || holder?.surface == null) {
            pendingIndex = i
            return
        }

        val mp = MediaPlayer()
        player = mp
        mp.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK)
        mp.setSurface(holder?.surface)
        mp.setAudioAttributes(
            android.media.AudioAttributes.Builder()
                .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MOVIE)
                .build()
        )
        mp.setOnPreparedListener {
            it.start()
            retries = 0
            showLoading(false)
            applyAspect()
        }
        mp.setOnVideoSizeChangedListener { _, w, h ->
            videoW = w
            videoH = h
            applyAspect()
        }
        mp.setOnInfoListener { _, what, _ ->
            when (what) {
                MediaPlayer.MEDIA_INFO_BUFFERING_START -> showLoading(true)
                MediaPlayer.MEDIA_INFO_BUFFERING_END,
                MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START -> showLoading(false)
            }
            true
        }
        mp.setOnErrorListener { _, what, extra ->
            onPlaybackError(what, extra)
            true
        }
        try {
            mp.setDataSource(
                this,
                Uri.parse(ch.url),
                mapOf("User-Agent" to "TV-AO-VIVO/0.1")
            )
            mp.prepareAsync()
        } catch (e: Exception) {
            onPlaybackError(-1, 0)
        }
    }

    private fun onPlaybackError(what: Int, extra: Int) {
        showLoading(false)
        if (retries < 2) {
            retries++
            showLoading(true, "Reconectando… ($retries/2)")
            ui.postDelayed(retryRunnable, 2500)
        } else {
            showLoading(false)
            showError("Não foi possível abrir este canal. O provedor pode estar fora do ar.")
        }
    }

    private fun releasePlayer() {
        val mp = player ?: return
        player = null
        try {
            mp.setOnPreparedListener(null)
            mp.setOnErrorListener(null)
            mp.setOnInfoListener(null)
            mp.reset()
        } catch (_: Exception) {
        }
        try {
            mp.release()
        } catch (_: Exception) {
        }
    }

    private fun applyAspect() {
        val w = surfaceBox.width
        val h = surfaceBox.height
        if (w <= 0 || h <= 0) return
        val vw = if (videoW > 0) videoW else 16
        val vh = if (videoH > 0) videoH else 9
        var outW = w
        var outH = h
        when (aspect) {
            ASPECT_FIT -> {
                val r = minOf(w.toFloat() / vw, h.toFloat() / vh)
                outW = (vw * r).toInt()
                outH = (vh * r).toInt()
            }
            ASPECT_16_9 -> {
                val r = minOf(w.toFloat() / 16f, h.toFloat() / 9f)
                outW = (16 * r).toInt()
                outH = (9 * r).toInt()
            }
            ASPECT_4_3 -> {
                val r = minOf(w.toFloat() / 4f, h.toFloat() / 3f)
                outW = (4 * r).toInt()
                outH = (3 * r).toInt()
            }
        }
        surfaceView.layoutParams = FrameLayout.LayoutParams(outW, outH, Gravity.CENTER)
        surfaceView.requestLayout()
    }

    // ------------------------------------------------------------------ teclas

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_CHANNEL_UP -> { zap(1); return true }
                KeyEvent.KEYCODE_CHANNEL_DOWN -> { zap(-1); return true }
            }
            val d = digit(event.keyCode)
            if (d >= 0) {
                pushDigit(d)
                return true
            }
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        showChrome()
        return when (keyCode) {
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                if (numberBuffer.visibility == View.VISIBLE) commitDigits() else togglePanel()
                true
            }
            KeyEvent.KEYCODE_INFO -> {
                cycleAspect()
                true
            }
            KeyEvent.KEYCODE_BACK -> {
                when {
                    numberBuffer.visibility == View.VISIBLE -> {
                        digitBuffer = ""
                        numberBuffer.visibility = View.GONE
                        true
                    }
                    panelVisible -> {
                        closePanel()
                        true
                    }
                    else -> super.onKeyDown(keyCode, event)
                }
            }
            KeyEvent.KEYCODE_DPAD_UP -> {
                if (!panelVisible) { zap(1); true } else super.onKeyDown(keyCode, event)
            }
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                if (!panelVisible) { zap(-1); true } else super.onKeyDown(keyCode, event)
            }
            KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_DOWN -> {
                val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
                am.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) AudioManager.ADJUST_RAISE
                    else AudioManager.ADJUST_LOWER,
                    0
                )
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    private fun digit(keyCode: Int): Int = when (keyCode) {
        in KeyEvent.KEYCODE_0..KeyEvent.KEYCODE_9 -> keyCode - KeyEvent.KEYCODE_0
        in KeyEvent.KEYCODE_NUMPAD_0..KeyEvent.KEYCODE_NUMPAD_9 -> keyCode - KeyEvent.KEYCODE_NUMPAD_0
        else -> -1
    }

    private fun pushDigit(d: Int) {
        if (digitBuffer.length >= 4) digitBuffer = ""
        digitBuffer += d.toString()
        numberBuffer.text = digitBuffer
        numberBuffer.visibility = View.VISIBLE
        ui.removeCallbacks(digitTimeout)
        ui.postDelayed(digitTimeout, 1800)
        showChrome()
    }

    private fun commitDigits() {
        ui.removeCallbacks(digitTimeout)
        val n = digitBuffer.toIntOrNull()
        digitBuffer = ""
        numberBuffer.visibility = View.GONE
        if (n == null) return
        val target = Library.channels.indexOfFirst { it.number == n }
        if (target >= 0) {
            closePanel()
            tune(target)
        } else {
            toast("Canal $n não existe")
        }
    }

    private fun zap(delta: Int) {
        tune(currentIndex + delta)
        showChrome()
    }

    // ------------------------------------------------------------------ painel e barra

    private fun togglePanel() {
        if (panelVisible) closePanel() else openPanel()
    }

    private fun openPanel() {
        panelShown = Library.channels
        if (panelShown.isEmpty()) return
        val fav = Favorites.load(this)
        updatePanelRows()
        panelList.adapter = ChannelAdapter(this, panelShown, fav,
            Library.channels.getOrNull(currentIndex)?.let { it.name + "|" + it.url })
        panelList.onItemClickListener = object : android.widget.AdapterView.OnItemClickListener {
            override fun onItemClick(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                val ch = panelShown.getOrNull(position) ?: return
                val idx = Library.channels.indexOfFirst { it.number == ch.number && it.url == ch.url }
                if (idx >= 0) {
                    closePanel()
                    tune(idx)
                }
            }
        }
        panelList.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                closePanel(); true
            } else false
        }
        panel.visibility = View.VISIBLE
        panelVisible = true
        topBar.visibility = View.VISIBLE
        infobar.visibility = View.VISIBLE
        ui.removeCallbacks(hideBar)
        panelList.setSelection(panelShown.indexOfFirst { ch ->
            Library.channels.getOrNull(currentIndex)?.let { it.number == ch.number } == true
        }.coerceAtLeast(0))
        panelList.requestFocus()
    }

    /** Texto das duas linhas fixas do painel: formato de imagem e favorito do canal atual. */
    private fun updatePanelRows() {
        aspectRow.text = "Imagem: ${ASPECT_NAMES[aspect]}"
        val ch = Library.channels.getOrNull(currentIndex)
        val fav = ch != null && Favorites.isFavorite(Favorites.load(this), ch)
        favRow.text = if (fav) "★ Favorito (OK remove)" else "☆ Favoritar canal"
        favRow.setTextColor(Color.parseColor(if (fav) "#FFD166" else Ui.TEXT))
    }

    private fun cycleAspect() {
        aspect = (aspect + 1) % ASPECT_NAMES.size
        applyAspect()
        updatePanelRows()
        toast("Imagem: ${ASPECT_NAMES[aspect]}")
    }

    private fun toggleFavorite() {
        val ch = Library.channels.getOrNull(currentIndex) ?: return
        val added = Favorites.toggle(this, ch)
        toast(if (added) "Adicionado aos favoritos" else "Removido dos favoritos")
        updatePanelRows()
    }

    private fun closePanel() {
        panel.visibility = View.GONE
        panelVisible = false
        hideChrome()
    }

    private fun showChrome() {
        topBar.visibility = View.VISIBLE
        infobar.visibility = View.VISIBLE
        ui.removeCallbacks(hideBar)
        ui.postDelayed(hideBar, 5000)
    }

    private fun hideChrome() {
        if (panelVisible) return
        topBar.visibility = View.GONE
        infobar.visibility = View.GONE
    }

    private fun updateInfo(ch: Channel) {
        infoNumber.text = ch.number.toString().padStart(2, '0')
        infoName.text = ch.name
        val total = Library.channels.size
        infoSub.text = "${ch.group}  ·  ${ch.number} de $total"
    }

    private fun showLoading(on: Boolean, text: String = "Sintonizando canal…") {
        loading.text = text
        loading.visibility = if (on) View.VISIBLE else View.GONE
    }

    private fun showError(message: String) {
        errorText.text = message
        errorBox.visibility = View.VISIBLE
        errorBox.post { (errorBox.getChildAt(1) as? View)?.requestFocus() }
    }

    private fun hideError() {
        errorBox.visibility = View.GONE
    }

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}

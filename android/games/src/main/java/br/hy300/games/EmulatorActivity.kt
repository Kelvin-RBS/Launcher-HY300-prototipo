package br.hy300.games

import android.app.Activity
import android.graphics.Color
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTimestamp
import android.media.AudioTrack
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import java.io.File

/**
 * Motor próprio: roda o jogo com o núcleo do Snes9x, sem controles de toque.
 * Menu em português: segure Select + Start (ou Voltar / botão Guide) para abrir.
 */
class EmulatorActivity : Activity(), SurfaceHolder.Callback {

    private companion object {
        const val SELECT_BIT = 1 shl 2
        const val START_BIT = 1 shl 3
        const val COMBO = SELECT_BIT or START_BIT  // Select + Start
    }

    private val lock = Any()
    private val ui = Handler(Looper.getMainLooper())
    private lateinit var game: Game
    private lateinit var root: FrameLayout
    private lateinit var surfaceView: SurfaceView
    private lateinit var menu: LinearLayout
    private lateinit var confirm: LinearLayout
    private lateinit var noButton: View
    private lateinit var messageView: TextView
    private lateinit var fpsView: TextView
    private val hideMessage = Runnable { messageView.visibility = View.GONE }

    @Volatile private var running = false
    @Volatile private var paused = false
    @Volatile private var menuVisible = false
    @Volatile private var coreReady = false
    @Volatile private var padMask = 0
    private var keyMask = 0
    private var axisMask = 0
    private var thread: Thread? = null
    private val probe = LatencyProbe()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val system = intent.getStringExtra("system") ?: "snes"
        val source = intent.getStringExtra("source") ?: "app"
        val rel = intent.getStringExtra("rel") ?: ""
        game = Game(system, Systems.label(system), source, rel, File(Catalog.baseFor(this, source), rel))
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        buildUi()
        running = true
        thread = Thread({ emuLoop() }, "emu").also { it.start() }
        say("Select + Start: menu  \u00B7  segure Start por 3 s: fechar o jogo")
    }

    override fun onResume() {
        super.onResume()
        hideSystemBars()
        applyAspect()
        if (!menuVisible) paused = false
    }

    override fun onPause() {
        super.onPause()
        paused = true
        if (coreReady) saveSram()
    }

    override fun onDestroy() {
        super.onDestroy()
        running = false
        try {
            thread?.join(2000)
        } catch (e: InterruptedException) {
            // segue
        }
    }

    @Suppress("DEPRECATION")
    private fun hideSystemBars() {
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
    }

    // ------------------------------------------------------------------ interface

    private fun buildUi() {
        root = FrameLayout(this)
        root.setBackgroundColor(Color.BLACK)

        surfaceView = SurfaceView(this)
        surfaceView.holder.addCallback(this)
        root.addView(surfaceView, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, Gravity.CENTER))

        fpsView = TextView(this).apply {
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.argb(140, 0, 0, 0))
            textSize = 13f
            setPadding(dp(8), dp(4), dp(8), dp(4))
            visibility = View.GONE
        }
        root.addView(fpsView, FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP or Gravity.START))

        messageView = TextView(this).apply {
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.argb(190, 6, 10, 20))
            textSize = 15f
            setPadding(dp(16), dp(8), dp(16), dp(8))
            visibility = View.GONE
        }
        root.addView(messageView, FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL).apply { bottomMargin = dp(24) })

        menu = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setBackgroundColor(Color.argb(235, 6, 10, 20))
            setPadding(dp(30), dp(20), dp(30), dp(20))
            visibility = View.GONE
        }
        menu.addView(TextView(this).apply {
            text = game.title
            textSize = 20f
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#EAF1FF"))
            setPadding(0, 0, 0, dp(6))
        })
        fun addItem(label: String, action: () -> Unit) {
            menu.addView(Ui.focusButton(this, label, action),
                LinearLayout.LayoutParams(dp(280), ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(8) })
        }
        addItem("Continuar") { closeMenu() }
        addItem("Reiniciar") {
            synchronized(lock) { NativeEmu.reset() }
            closeMenu()
        }
        addItem("Salvar estado") { saveState() }
        addItem("Carregar estado") { loadState() }
        addItem("Ajustes") { startActivity(android.content.Intent(this, EmulatorSettingsActivity::class.java)) }
        addItem("Diagnóstico de atraso") { showDiagnostic() }
        addItem("Sair para a lista de jogos") { finish() }
        root.addView(menu, FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER))

        // Pergunta "Fechar o jogo?" (abre ao segurar Start por 3 segundos)
        confirm = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setBackgroundColor(Color.argb(240, 6, 10, 20))
            setPadding(dp(30), dp(22), dp(30), dp(22))
            visibility = View.GONE
        }
        confirm.addView(TextView(this).apply {
            text = "Fechar o jogo?"
            textSize = 24f
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#EAF1FF"))
        })
        confirm.addView(TextView(this).apply {
            text = game.title + "\nO save do jogo é mantido. Estados não salvos serão perdidos."
            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#C5D2EA"))
            setPadding(0, dp(8), 0, dp(6))
        })
        noButton = Ui.focusButton(this, "Não, continuar") { closeMenu() }
        confirm.addView(noButton, LinearLayout.LayoutParams(dp(300), ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(10) })
        confirm.addView(Ui.focusButton(this, "Sim, fechar o jogo") { finish() },
            LinearLayout.LayoutParams(dp(300), ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(8) })
        root.addView(confirm, FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER))

        setContentView(root)
    }

    /** Ajusta o tamanho da imagem conforme a proporção escolhida (o sistema faz a escala). */
    private fun applyAspect() {
        val dm = resources.displayMetrics
        val sw = dm.widthPixels.toFloat()
        val sh = dm.heightPixels.toFloat()
        val target = when (EmuPrefs.aspect(this)) {
            0 -> 4f / 3f
            1 -> 8f / 7f
            else -> sw / sh
        }
        var w = sh * target
        var h = sh
        if (w > sw) {
            w = sw
            h = sw / target
        }
        surfaceView.layoutParams = FrameLayout.LayoutParams(w.toInt(), h.toInt(), Gravity.CENTER)
    }

    private fun say(text: String) {
        messageView.text = text
        messageView.visibility = View.VISIBLE
        ui.removeCallbacks(hideMessage)
        ui.postDelayed(hideMessage, 2500)
    }

    private fun openMenu() {
        if (menuVisible) return
        menuVisible = true
        paused = true
        keyMask = 0
        axisMask = 0
        padMask = 0
        if (coreReady) saveSram()
        menu.visibility = View.VISIBLE
        menu.getChildAt(1).requestFocus()
    }

    private fun openCloseDialog() {
        if (menuVisible) return
        menuVisible = true
        paused = true
        keyMask = 0
        axisMask = 0
        padMask = 0
        if (coreReady) saveSram()
        confirm.visibility = View.VISIBLE
        noButton.requestFocus()
    }

    private fun closeMenu() {
        confirm.visibility = View.GONE
        menu.visibility = View.GONE
        menuVisible = false
        paused = false
    }

    // ------------------------------------------------------------------ saves

    private fun stateFile(): File {
        val dir = File(filesDir, "states")
        dir.mkdirs()
        return File(dir, game.file.nameWithoutExtension + ".state")
    }

    private fun sramFile(): File {
        val dir = File(filesDir, "saves")
        dir.mkdirs()
        return File(dir, game.file.nameWithoutExtension + ".srm")
    }

    private fun saveSram() {
        val data = synchronized(lock) { if (coreReady) NativeEmu.sram() else null } ?: return
        try {
            sramFile().writeBytes(data)
        } catch (e: Exception) {
            // sem espaço ou sem permissão: ignora
        }
    }

    private fun saveState() {
        val data = synchronized(lock) { if (coreReady) NativeEmu.serializeState() else null }
        if (data == null) {
            say("Não foi possível salvar")
            return
        }
        try {
            stateFile().writeBytes(data)
            say("Estado salvo")
        } catch (e: Exception) {
            say("Não foi possível salvar")
        }
    }

    private fun loadState() {
        val f = stateFile()
        if (!f.isFile) {
            say("Nenhum estado salvo")
            return
        }
        val ok = try {
            val bytes = f.readBytes()
            synchronized(lock) { coreReady && NativeEmu.unserializeState(bytes) }
        } catch (e: Exception) {
            false
        }
        if (ok) {
            closeMenu()
            say("Estado carregado")
        } else {
            say("Não foi possível carregar")
        }
    }

    // ------------------------------------------------------------------ emulação

    /** Mostra o relatório de atraso (e o salva em Jogos/diagnostico-atraso.txt para ler pelo PC). */
    private fun showDiagnostic() {
        val stamp = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
        var text = probe.report(game.title, stamp)
        val saved = saveReport(text)
        text += "\n" + (if (saved != null) "Relatório salvo em: " + saved else "Não foi possível salvar o relatório.")
        val tv = TextView(this).apply {
            this.text = text
            typeface = android.graphics.Typeface.MONOSPACE
            textSize = 12f
            setTextColor(Color.parseColor("#EAF1FF"))
            setPadding(dp(16), dp(8), dp(16), dp(8))
        }
        val scroll = android.widget.ScrollView(this).apply {
            isFocusable = true
            addView(tv)
        }
        android.app.AlertDialog.Builder(this)
            .setTitle("Diagnóstico de atraso")
            .setView(scroll)
            .setPositiveButton("Fechar", null)
            .show()
        scroll.requestFocus()
    }

    private fun saveReport(text: String): String? {
        val targets = ArrayList<File>()
        if (Catalog.canReadPublic()) targets.add(File(Catalog.PUBLIC_DIR, "diagnostico-atraso.txt"))
        getExternalFilesDir(null)?.let { targets.add(File(it, "diagnostico-atraso.txt")) }
        for (f in targets) {
            try {
                f.parentFile?.mkdirs()
                f.writeText(text, Charsets.UTF_8)
                return f.absolutePath
            } catch (e: Exception) {
                // tenta o próximo lugar
            }
        }
        return null
    }

    /** Falha com explicação: mostra uma janela com o motivo (OK ou Voltar fecham o jogo). */
    private fun failDialog(title: String, detail: String) {
        ui.post {
            android.app.AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(detail)
                .setPositiveButton("OK") { _, _ -> finish() }
                .setOnCancelListener { finish() }
                .show()
        }
    }

    private fun fail(message: String) {
        ui.post {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            finish()
        }
    }

    /** Quadros de áudio equivalentes a [n] quadros de vídeo, na taxa [rate]. */
    private fun videoFrames(rate: Int, n: Int): Int = (rate / 60) * n

    /**
     * Áudio na taxa nativa do aparelho (o motor converte): só assim o Android usa o caminho de
     * baixa latência. A capacidade é grande, mas o tamanho em uso começa pequeno e só cresce se
     * o som falhar (ver emuLoop).
     */
    private fun createAudio(rate: Int): AudioTrack {
        val minBuf = AudioTrack.getMinBufferSize(rate, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT)
        val capacityFrames = Math.max(minBuf / 4, videoFrames(rate, 10))
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(rate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                    .build()
            )
            .setBufferSizeInBytes(capacityFrames * 4)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
            .build()
        track.setBufferSizeInFrames(videoFrames(rate, 2 shl EmuPrefs.latency(this)))  // 2, 4 ou 8 quadros de vídeo
        return track
    }

    private fun emuLoop() {
        try {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO)
        } catch (e: Exception) {
            // segue com a prioridade normal
        }
        val corePath = Engine.corePath(this, game.system)
        if (corePath == null) {
            fail("Núcleo do emulador não encontrado")
            return
        }
        val saves = File(filesDir, "saves")
        saves.mkdirs()
        if (!NativeEmu.loadCore(corePath, saves.absolutePath)) {
            fail("Não foi possível carregar o núcleo: " + NativeEmu.lastError())
            return
        }
        val rom = RomLoader.readRom(game.file)
        if (rom == null) {
            NativeEmu.unload()
            fail("Não foi possível ler o jogo")
            return
        }
        val romBytes = RomLoader.stripHeader(rom.second, game.system)
        if (!NativeEmu.loadGame(romBytes, rom.first)) {
            val log = NativeEmu.lastLog().trim()
            val detail = RomLoader.describe(romBytes, rom.first, game.system) +
                (if (log.isNotEmpty()) "\n\nO núcleo disse:\n" + log.takeLast(600) else "\n\nO núcleo não explicou o motivo.")
            NativeEmu.unload()
            failDialog("O núcleo não aceitou este jogo", detail)
            return
        }
        coreReady = true
        val sram = sramFile()
        if (sram.isFile) {
            try {
                val bytes = sram.readBytes()
                synchronized(lock) { NativeEmu.setSram(bytes) }
            } catch (e: Exception) {
                // segue sem o save
            }
        }

        val av = NativeEmu.avInfo()
        val coreRate = if (av[1] > 8000.0) av[1] else 32040.0
        val nativeRate = AudioTrack.getNativeOutputSampleRate(AudioManager.STREAM_MUSIC)
        val rate = if (nativeRate in 8000..192000) nativeRate else 48000
        NativeEmu.setAudioRates(coreRate, rate.toDouble())
        ui.post { applyAspect() }
        val audio = createAudio(rate)
        val maxBufFrames = videoFrames(rate, 8)
        audio.play()

        // Informações para o diagnóstico de atraso
        NativeEmu.setVideoTrim(EmuPrefs.videoTrim(this))
        probe.videoTrim = EmuPrefs.videoTrim(this) == 1
        probe.coreFps = if (av[0] > 10.0) av[0] else 60.0
        probe.nativeRate = rate
        probe.fastTrack = try {
            audio.getPerformanceMode() == AudioTrack.PERFORMANCE_MODE_LOW_LATENCY
        } catch (e: Exception) {
            null
        }
        try {
            val am = getSystemService(AUDIO_SERVICE) as AudioManager
            probe.burstFrames = am.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER)?.toIntOrNull() ?: 0
            probe.lowLatencyFeature = packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_AUDIO_LOW_LATENCY)
        } catch (e: Exception) {
            // segue sem essas informações
        }
        ui.post { probe.displayHz = windowManager.defaultDisplay.refreshRate }
        var written = 0L       // quadros de áudio entregues ao sistema desde a última retomada
        var headBase = 0L      // posição de reprodução na última retomada
        var needBase = false

        val buf = ShortArray(8192)
        var wasPaused = false
        var frames = 0
        var lastFps = System.nanoTime()
        var worstMs = 0.0          // quadro de emulação mais lento no último segundo
        var lastUnderruns = -1     // -1 = ainda sem referência (ignora a falha natural do início)
        var comboSince = 0L
        var startSince = 0L
        while (running) {
            if (paused) {
                if (!wasPaused) {
                    audio.pause()
                    audio.flush()
                    wasPaused = true
                    needBase = true
                }
                try {
                    Thread.sleep(30)
                } catch (e: InterruptedException) {
                    // segue
                }
                continue
            }
            if (wasPaused) {
                if (needBase) {
                    headBase = audio.getPlaybackHeadPosition().toLong() and 0xFFFFFFFFL
                    written = 0L
                    needBase = false
                }
                probe.resetFramePeriod()
                audio.play()
                wasPaused = false
                lastUnderruns = -1
            }
            val mask = padMask
            probe.consumePress(android.os.SystemClock.uptimeMillis())
            probe.frameStart(System.nanoTime())
            if ((mask and COMBO) == COMBO) {
                val now = System.nanoTime()
                if (comboSince == 0L) {
                    comboSince = now
                } else if (now - comboSince > 800_000_000L) {
                    comboSince = 0L
                    ui.post { openMenu() }
                    continue
                }
            } else {
                comboSince = 0L
            }
            // Start segurado sozinho por 3 segundos: pergunta se quer fechar o jogo
            if ((mask and START_BIT) != 0 && (mask and SELECT_BIT) == 0) {
                val now = System.nanoTime()
                if (startSince == 0L) {
                    startSince = now
                } else if (now - startSince > 3_000_000_000L) {
                    startSince = 0L
                    ui.post { openCloseDialog() }
                    continue
                }
            } else {
                startSince = 0L
            }
            val t0 = System.nanoTime()
            val n = synchronized(lock) {
                NativeEmu.setInput(0, mask)
                NativeEmu.runFrame(buf)
            }
            val emuMs = (System.nanoTime() - t0) / 1_000_000.0
            if (emuMs > worstMs) worstMs = emuMs
            probe.addEmu(emuMs)
            if (n > 0) {
                audio.write(buf, 0, n)  // bloqueia quando o buffer enche: dá o ritmo da emulação
                written += n / 2
            }
            frames++
            val t = System.nanoTime()
            if (t - lastFps >= 1_000_000_000L) {
                val fps = frames
                frames = 0
                lastFps = t
                // Se o som falhou (buffer vazio), aumenta o buffer em 1 quadro de vídeo, até o limite
                val under = audio.underrunCount
                val curBuf = audio.getBufferSizeInFrames()
                if (lastUnderruns >= 0 && under > lastUnderruns && curBuf < maxBufFrames) {
                    audio.setBufferSizeInFrames(Math.min(maxBufFrames, curBuf + videoFrames(rate, 1)))
                }
                lastUnderruns = under
                val bufMs = audio.getBufferSizeInFrames() * 1000 / rate
                probe.underruns = under
                probe.bufferMs = bufMs
                probe.fps = fps
                // Atraso real do áudio: quadros entregues menos quadros já tocados (posição medida pelo sistema)
                try {
                    val ts = AudioTimestamp()
                    if (audio.getTimestamp(ts)) {
                        val played = ts.framePosition + (System.nanoTime() - ts.nanoTime) * rate / 1_000_000_000L
                        val queued = headBase + written - played
                        if (queued in 0..rate.toLong()) probe.addAudioLatency(queued * 1000.0 / rate)
                    }
                } catch (e: Exception) {
                    // sem medição neste segundo
                }
                val vs = NativeEmu.videoStats()
                probe.addVideo(vs[0], vs[1], vs[5].toLong(), vs[4].toLong())
                if (probe.outputType < 0) {
                    try {
                        val d = audio.getRoutedDevice()
                        if (d != null) {
                            probe.outputType = d.type
                            probe.outputName = d.productName?.toString() ?: ""
                        }
                    } catch (e: Exception) {
                        // segue sem saber a saída
                    }
                }
                val worst = worstMs
                worstMs = 0.0
                val show = EmuPrefs.showFps(this) == 1
                ui.post {
                    fpsView.text = fps.toString() + " fps  \u00B7  áudio " + bufMs + " ms  \u00B7  falhas " + under +
                        "  \u00B7  emulação máx " + String.format("%.1f", worst) + " ms\n" + probe.overlayLine()
                    fpsView.visibility = if (show) View.VISIBLE else View.GONE
                }
            }
        }
        saveSram()
        synchronized(lock) {
            coreReady = false
            NativeEmu.unload()
        }
        audio.release()
    }

    // ------------------------------------------------------------------ superfície

    override fun surfaceCreated(holder: SurfaceHolder) {
        NativeEmu.setSurface(holder.surface)
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        NativeEmu.setSurface(null)
    }

    // ------------------------------------------------------------------ controles

    /** Bit do botão do SNES (ordem do libretro): B0 Y1 Select2 Start3 Cima4 Baixo5 Esq6 Dir7 A8 X9 L10 R11. */
    private fun bitFor(keyCode: Int): Int {
        val nintendo = EmuPrefs.layout(this) == 1
        return when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> 4
            KeyEvent.KEYCODE_DPAD_DOWN -> 5
            KeyEvent.KEYCODE_DPAD_LEFT -> 6
            KeyEvent.KEYCODE_DPAD_RIGHT -> 7
            KeyEvent.KEYCODE_BUTTON_A -> if (nintendo) 8 else 0
            KeyEvent.KEYCODE_BUTTON_B -> if (nintendo) 0 else 8
            KeyEvent.KEYCODE_BUTTON_X -> if (nintendo) 9 else 1
            KeyEvent.KEYCODE_BUTTON_Y -> if (nintendo) 1 else 9
            KeyEvent.KEYCODE_BUTTON_L1, KeyEvent.KEYCODE_BUTTON_L2 -> 10
            KeyEvent.KEYCODE_BUTTON_R1, KeyEvent.KEYCODE_BUTTON_R2 -> 11
            KeyEvent.KEYCODE_BUTTON_START -> 3
            KeyEvent.KEYCODE_BUTTON_SELECT -> 2
            else -> -1
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val code = event.keyCode
        if (menuVisible) {
            if (event.action == KeyEvent.ACTION_UP) {
                if (code == KeyEvent.KEYCODE_BACK || code == KeyEvent.KEYCODE_BUTTON_B) {
                    closeMenu()
                    return true
                }
                if (code == KeyEvent.KEYCODE_BUTTON_A) {
                    currentFocus?.performClick()
                    return true
                }
            }
            return super.dispatchKeyEvent(event)
        }
        if (code == KeyEvent.KEYCODE_BACK || code == KeyEvent.KEYCODE_BUTTON_MODE) {
            if (event.action == KeyEvent.ACTION_UP) openMenu()
            return true
        }
        val bit = bitFor(code)
        if (bit >= 0) {
            if (event.action == KeyEvent.ACTION_DOWN) {
                if (event.repeatCount == 0) probe.notePress(event.eventTime, false)
                keyMask = keyMask or (1 shl bit)
            } else if (event.action == KeyEvent.ACTION_UP) {
                keyMask = keyMask and (1 shl bit).inv()
            }
            padMask = keyMask or axisMask
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
        val joystick = (event.source and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK
        if (!menuVisible && joystick && event.action == MotionEvent.ACTION_MOVE) {
            // cadência real do controle (inclui os eventos que o Android juntou em um só)
            for (h in 0 until event.historySize) probe.noteReport(event.getHistoricalEventTime(h))
            probe.noteReport(event.eventTime)
            val hx = event.getAxisValue(MotionEvent.AXIS_HAT_X)
            val hy = event.getAxisValue(MotionEvent.AXIS_HAT_Y)
            val lx = event.getAxisValue(MotionEvent.AXIS_X)
            val ly = event.getAxisValue(MotionEvent.AXIS_Y)
            var m = 0
            if (hx < -0.5f || lx < -0.5f) m = m or (1 shl 6)
            if (hx > 0.5f || lx > 0.5f) m = m or (1 shl 7)
            if (hy < -0.5f || ly < -0.5f) m = m or (1 shl 4)
            if (hy > 0.5f || ly > 0.5f) m = m or (1 shl 5)
            if (event.getAxisValue(MotionEvent.AXIS_LTRIGGER) > 0.5f || event.getAxisValue(MotionEvent.AXIS_BRAKE) > 0.5f) m = m or (1 shl 10)
            if (event.getAxisValue(MotionEvent.AXIS_RTRIGGER) > 0.5f || event.getAxisValue(MotionEvent.AXIS_GAS) > 0.5f) m = m or (1 shl 11)
            if ((m and axisMask.inv()) != 0) probe.notePress(event.eventTime, true)
            axisMask = m
            padMask = keyMask or axisMask
            return true
        }
        return super.dispatchGenericMotionEvent(event)
    }
}

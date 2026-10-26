package br.hy300.games

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.hardware.input.InputManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

/** Configurações essenciais da fase 1: controles, pasta dos jogos e acesso aos ajustes do emulador. */
class SettingsActivity : Activity() {

    private lateinit var controllersText: TextView
    private lateinit var filesText: TextView
    private lateinit var inputManager: InputManager
    private val handler = Handler(Looper.getMainLooper())

    private val listener = object : InputManager.InputDeviceListener {
        override fun onInputDeviceAdded(deviceId: Int) { refreshControllers() }
        override fun onInputDeviceChanged(deviceId: Int) { refreshControllers() }
        override fun onInputDeviceRemoved(deviceId: Int) { refreshControllers() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        inputManager = getSystemService(Context.INPUT_SERVICE) as InputManager
        setContentView(buildUi())
    }

    override fun onStart() {
        super.onStart()
        inputManager.registerInputDeviceListener(listener, handler)
        refreshControllers()
        refreshFiles()
    }

    override fun onStop() {
        super.onStop()
        inputManager.unregisterInputDeviceListener(listener)
    }

    private fun label(text: String, size: Float, color: String, top: Int = 0): TextView = TextView(this).apply {
        this.text = text
        textSize = size
        setTextColor(Color.parseColor(color))
        setPadding(0, dp(top), 0, 0)
    }

    private fun buildUi(): ScrollView {
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(43), dp(24), dp(43), dp(24))
        }
        col.addView(label("Configurações", 28f, "#EAF1FF"))

        col.addView(label("CONTROLES", 11f, "#A9B8D2", 16))
        controllersText = label("", 16f, "#EAF1FF", 4)
        col.addView(controllersText)
        col.addView(label("Funcionam controles Bluetooth e USB em geral: Xbox, Nintendo Switch, PlayStation e genéricos.", 13f, "#A9B8D2", 4))

        col.addView(label("PASTA DOS JOGOS", 11f, "#A9B8D2", 16))
        col.addView(label(Catalog.PUBLIC_DIR, 14f, "#EAF1FF", 4))
        filesText = label("", 13f, "#A9B8D2", 4)
        col.addView(filesText)

        col.addView(label("EMULADOR", 11f, "#A9B8D2", 16))
        val version = Emulator.versionName(packageManager)
        col.addView(label(
            if (version == null) "Snes9x EX+ não instalado" else "Snes9x EX+ " + version + " (instalado)",
            16f, "#EAF1FF", 4
        ))
        col.addView(label("Vídeo, áudio e mapeamento de botões ficam nos ajustes do próprio emulador. Os ajustes finos para este projetor virão com o motor próprio (fase 2).", 13f, "#A9B8D2", 4))

        val buttons = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(20), 0, 0)
        }
        val pair = Button(this).apply {
            text = "Parear controle"
            isAllCaps = false
            setOnClickListener { openBluetoothSettings() }
        }
        val emu = Button(this).apply {
            text = "Ajustes do emulador"
            isAllCaps = false
            setOnClickListener {
                if (!Emulator.openEmulatorMenu(this@SettingsActivity)) {
                    Toast.makeText(this@SettingsActivity, "Emulador não encontrado", Toast.LENGTH_SHORT).show()
                }
            }
        }
        val back = Button(this).apply {
            text = "Voltar"
            isAllCaps = false
            setOnClickListener { finish() }
        }
        buttons.addView(pair, LinearLayout.LayoutParams(dp(170), ViewGroup.LayoutParams.WRAP_CONTENT).apply { marginEnd = dp(12) })
        buttons.addView(emu, LinearLayout.LayoutParams(dp(190), ViewGroup.LayoutParams.WRAP_CONTENT).apply { marginEnd = dp(12) })
        buttons.addView(back, LinearLayout.LayoutParams(dp(120), ViewGroup.LayoutParams.WRAP_CONTENT))
        col.addView(buttons)
        pair.requestFocus()

        return ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#060A14"))
            addView(col)
        }
    }

    private fun refreshFiles() {
        filesText.text = if (Catalog.canReadPublic()) {
            "Acesso a arquivos: liberado"
        } else {
            "Acesso a arquivos NÃO liberado. No computador: adb shell appops set br.hy300.games MANAGE_EXTERNAL_STORAGE allow"
        }
    }

    private fun refreshControllers() {
        val pads = Controllers.connected()
        controllersText.text = if (pads.isEmpty()) {
            "Nenhum controle conectado"
        } else {
            pads.joinToString("\n") { it.name }
        }
    }

    private fun openBluetoothSettings() {
        try {
            startActivity(
                Intent().setComponent(ComponentName("com.htc.htcsettings", "com.htc.activity.BluetoothSettingActivity"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (e: Exception) {
            Toast.makeText(this, "Não foi possível abrir o Bluetooth", Toast.LENGTH_SHORT).show()
        }
    }
}

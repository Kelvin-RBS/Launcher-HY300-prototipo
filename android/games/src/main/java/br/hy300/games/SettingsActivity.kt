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
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

/** Configurações do app de jogos: controles, pasta dos jogos, motor e acesso aos ajustes do emulador. */
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

    private fun engineText(): String {
        if (Engine.ownAvailable(this)) {
            return "Motor próprio: Snes9x HY300 (núcleo " + Engine.coreName(this) + ")"
        }
        val version = Emulator.versionName(packageManager)
        return "Motor próprio indisponível (núcleo não instalado). Usando o Snes9x EX+ " +
            (version ?: "(não instalado)")
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
        col.addView(label(engineText(), 15f, "#EAF1FF", 4))
        col.addView(label("Durante o jogo: Select + Start abre o menu; segurar Start por 3 segundos pergunta se quer fechar o jogo e voltar à lista.", 13f, "#A9B8D2", 4))

        val buttons = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(20), 0, 0)
        }
        val pair = Ui.focusButton(this, "Parear controle") { openBluetoothSettings() }
        val emu = Ui.focusButton(this, "Ajustes do emulador") {
            startActivity(Intent(this, EmulatorSettingsActivity::class.java))
        }
        val back = Ui.focusButton(this, "Voltar") { finish() }
        buttons.addView(pair, LinearLayout.LayoutParams(dp(170), ViewGroup.LayoutParams.WRAP_CONTENT).apply { marginEnd = dp(14) })
        buttons.addView(emu, LinearLayout.LayoutParams(dp(210), ViewGroup.LayoutParams.WRAP_CONTENT).apply { marginEnd = dp(14) })
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

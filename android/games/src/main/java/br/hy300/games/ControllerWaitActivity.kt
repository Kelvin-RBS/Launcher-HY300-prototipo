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
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import java.io.File

/** Pede para conectar um controle e abre o jogo sozinho assim que ele conectar. OK volta à lista. */
class ControllerWaitActivity : Activity() {

    private lateinit var game: Game
    private lateinit var inputManager: InputManager
    private val handler = Handler(Looper.getMainLooper())
    private var started = false

    private val listener = object : InputManager.InputDeviceListener {
        override fun onInputDeviceAdded(deviceId: Int) { checkController() }
        override fun onInputDeviceChanged(deviceId: Int) { checkController() }
        override fun onInputDeviceRemoved(deviceId: Int) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val system = intent.getStringExtra("system") ?: "snes"
        val source = intent.getStringExtra("source") ?: "app"
        val rel = intent.getStringExtra("rel") ?: ""
        game = Game(system, Systems.label(system), source, rel, File(Catalog.baseFor(this, source), rel))
        inputManager = getSystemService(Context.INPUT_SERVICE) as InputManager
        setContentView(buildUi())
    }

    override fun onStart() {
        super.onStart()
        inputManager.registerInputDeviceListener(listener, handler)
        checkController()
    }

    override fun onStop() {
        super.onStop()
        inputManager.unregisterInputDeviceListener(listener)
    }

    private fun checkController() {
        if (started || Controllers.connected().isEmpty()) return
        started = true
        if (!Emulator.play(this, game)) {
            Toast.makeText(this, "Não foi possível abrir o jogo", Toast.LENGTH_LONG).show()
        }
        finish()
    }

    private fun buildUi(): LinearLayout {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#060A14"))
            setPadding(dp(43), dp(24), dp(43), dp(24))
        }
        root.addView(ImageView(this).apply { setImageResource(R.drawable.ic_gamepad) },
            LinearLayout.LayoutParams(dp(72), dp(72)))
        root.addView(TextView(this).apply {
            text = "Conecte um controle"
            textSize = 30f
            setTextColor(Color.parseColor("#EAF1FF"))
            gravity = Gravity.CENTER
            setPadding(0, dp(12), 0, dp(8))
        })
        root.addView(TextView(this).apply {
            text = "Ligue um controle Bluetooth (Xbox, Nintendo Switch, PlayStation, de PC...) e coloque-o em modo de pareamento.\nO jogo abre sozinho quando ele conectar."
            textSize = 15f
            setTextColor(Color.parseColor("#D5DFF2"))
            gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(dp(560), ViewGroup.LayoutParams.WRAP_CONTENT))
        root.addView(TextView(this).apply {
            text = "Aguardando controle..."
            textSize = 14f
            setTextColor(Color.parseColor("#A9B8D2"))
            gravity = Gravity.CENTER
            setPadding(0, dp(16), 0, dp(16))
        })

        val buttons = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        val ok = Ui.focusButton(this, "OK") { finish() }
        val pair = Ui.focusButton(this, "Parear controle") { openBluetoothSettings() }
        buttons.addView(ok, LinearLayout.LayoutParams(dp(140), ViewGroup.LayoutParams.WRAP_CONTENT).apply { marginEnd = dp(14) })
        buttons.addView(pair, LinearLayout.LayoutParams(dp(190), ViewGroup.LayoutParams.WRAP_CONTENT))
        root.addView(buttons)
        ok.requestFocus()
        return root
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

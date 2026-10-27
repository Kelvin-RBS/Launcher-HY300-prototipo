package br.hy300.games

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/** Ajustes do emulador próprio (em português, só o essencial, sem controles de toque). */
class EmulatorSettingsActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(43), dp(24), dp(43), dp(24))
        }
        col.addView(TextView(this).apply {
            text = "Ajustes do emulador"
            textSize = 28f
            setTextColor(Color.parseColor("#EAF1FF"))
        })
        col.addView(TextView(this).apply {
            text = if (Engine.ownAvailable(this@EmulatorSettingsActivity)) {
                "Snes9x HY300 (núcleo " + Engine.coreName(this@EmulatorSettingsActivity) + "). Esquerda e direita mudam o valor."
            } else {
                "O motor próprio ainda não está instalado: estes ajustes valem quando o núcleo for adicionado."
            }
            textSize = 13f
            setTextColor(Color.parseColor("#A9B8D2"))
            setPadding(0, dp(4), 0, dp(14))
        })

        val rowLp = { LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48)).apply { topMargin = dp(8) } }
        val first = Ui.optionRow(this, "Proporção da tela", listOf("4:3 (padrão)", "8:7 (pixel exato)", "Tela cheia"),
            { EmuPrefs.aspect(this) }, { EmuPrefs.setAspect(this, it) })
        col.addView(first, rowLp())
        col.addView(Ui.optionRow(this, "Botões do controle", listOf("Xbox / PC (por posição)", "Nintendo (por rótulo)"),
            { EmuPrefs.layout(this) }, { EmuPrefs.setLayout(this, it) }), rowLp())
        col.addView(Ui.optionRow(this, "Latência do áudio", listOf("Baixa", "Média", "Alta"),
            { EmuPrefs.latency(this) }, { EmuPrefs.setLatency(this, it) }), rowLp())
        col.addView(Ui.optionRow(this, "Mostrar quadros por segundo", listOf("Não", "Sim"),
            { EmuPrefs.showFps(this) }, { EmuPrefs.setShowFps(this, it) }), rowLp())

        col.addView(TextView(this).apply {
            text = "Dica: se o som falhar ou travar, aumente a latência do áudio. Se os botões A e B parecerem trocados, mude \"Botões do controle\"."
            textSize = 13f
            setTextColor(Color.parseColor("#A9B8D2"))
            setPadding(0, dp(12), 0, 0)
        })
        val back = Ui.focusButton(this, "Voltar") { finish() }
        col.addView(back, LinearLayout.LayoutParams(dp(140), ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(16) })

        setContentView(ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#060A14"))
            addView(col)
        })
        first.requestFocus()
    }
}

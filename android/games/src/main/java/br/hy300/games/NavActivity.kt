package br.hy300.games

import android.app.Activity
import android.os.SystemClock
import android.view.KeyEvent
import android.view.View

/**
 * Base de todas as telas: o MESMO tique ao mover o cursor, e um único som por movimento.
 *
 * Por que o app assume a navegação do direcional: quando o direcional move o foco, o próprio
 * Android toca um som de navegação (sem consultar o app). Para ter só o tique do app, as teclas
 * direcionais que nenhuma tela tratou são atendidas aqui; assim o Android não faz a navegação
 * dele e não toca o som dele.
 */
open class NavActivity : Activity() {
    private var sfx: Sfx? = null
    private var quietUntil = 0L

    override fun onContentChanged() {
        super.onContentChanged()
        if (sfx != null) return
        val s = Sfx(this)
        sfx = s
        // Qualquer mudança de foco na tela toca o tique (menos as que o app faz por conta própria, ver quiet)
        window.decorView.viewTreeObserver.addOnGlobalFocusChangeListener { _, newFocus ->
            if (newFocus != null && SystemClock.uptimeMillis() >= quietUntil) s.tick()
        }
    }

    override fun onResume() {
        super.onResume()
        quiet(700)  // o foco que o sistema devolve ao voltar para a tela não toca
    }

    override fun onDestroy() {
        super.onDestroy()
        sfx?.release()
        sfx = null
    }

    /** Silencia o tique pelos próximos [ms] milissegundos (foco colocado pelo app, não pelo usuário). */
    protected fun quiet(ms: Long = 300L) {
        quietUntil = SystemClock.uptimeMillis() + ms
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (super.dispatchKeyEvent(event)) return true  // as telas e os componentes têm a vez primeiro
        return navigate(event)
    }

    private fun navigate(event: KeyEvent): Boolean {
        val dir = when (event.keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> View.FOCUS_UP
            KeyEvent.KEYCODE_DPAD_DOWN -> View.FOCUS_DOWN
            KeyEvent.KEYCODE_DPAD_LEFT -> View.FOCUS_LEFT
            KeyEvent.KEYCODE_DPAD_RIGHT -> View.FOCUS_RIGHT
            else -> return false
        }
        if (event.action != KeyEvent.ACTION_DOWN) return false
        val cur = currentFocus ?: return false
        val next = cur.focusSearch(dir) ?: return false
        if (next === cur) return false
        return next.requestFocus(dir)
    }
}

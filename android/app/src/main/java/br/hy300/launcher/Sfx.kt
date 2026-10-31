package br.hy300.launcher

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.SystemClock

/**
 * Tique curto ao mover o cursor. Usa o SoundPool do próprio app, então não depende do ajuste
 * "efeitos sonoros" do sistema (que costuma vir desligado em projetor).
 */
class Sfx(ctx: Context) {
    private val pool: SoundPool = SoundPool.Builder()
        .setMaxStreams(2)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)  // segue o volume principal do projetor
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()
    private var soundId = 0
    @Volatile private var loaded = false

    /** Enquanto for true, o tique não toca (usado quando o foco é colocado por código, não pelo usuário). */
    @Volatile var muted = false

    init {
        pool.setOnLoadCompleteListener { _, id, status -> if (id == soundId && status == 0) loaded = true }
        soundId = pool.load(ctx, R.raw.nav_tick, 1)
    }

    private var lastTick = 0L

    fun tick() {
        val now = SystemClock.uptimeMillis()
        // Nunca toca duas vezes em menos de 60 ms (garantia contra tique duplicado)
        if (!loaded || muted || now - lastTick < 60) return
        lastTick = now
        pool.play(soundId, 0.6f, 0.6f, 1, 0, 1f)
    }

    fun release() {
        pool.release()
    }
}

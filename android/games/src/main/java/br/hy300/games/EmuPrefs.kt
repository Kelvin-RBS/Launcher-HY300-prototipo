package br.hy300.games

import android.content.Context

/** Ajustes do emulador próprio. */
object EmuPrefs {
    private fun sp(ctx: Context) = ctx.getSharedPreferences("emu", Context.MODE_PRIVATE)

    /** 0 = 4:3, 1 = 8:7 (pixel exato), 2 = tela cheia */
    fun aspect(ctx: Context): Int = sp(ctx).getInt("aspect", 0)
    fun setAspect(ctx: Context, v: Int) = sp(ctx).edit().putInt("aspect", v).apply()

    /** 0 = botões por posição (Xbox/PC), 1 = rótulos Nintendo */
    fun layout(ctx: Context): Int = sp(ctx).getInt("layout", 0)
    fun setLayout(ctx: Context, v: Int) = sp(ctx).edit().putInt("layout", v).apply()

    /** 0 = baixa, 1 = média, 2 = alta (maior = mais estável, com mais atraso) */
    fun latency(ctx: Context): Int = sp(ctx).getInt("latency", 1)
    fun setLatency(ctx: Context, v: Int) = sp(ctx).edit().putInt("latency", v).apply()

    /** 1 = mostrar quadros por segundo */
    fun showFps(ctx: Context): Int = sp(ctx).getInt("fps", 0)
    fun setShowFps(ctx: Context, v: Int) = sp(ctx).edit().putInt("fps", v).apply()
}

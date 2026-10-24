package br.hy300.launcher

import android.content.Context

/** Converte dp em pixels. */
fun Context.dp(v: Int): Int = (v * resources.displayMetrics.density + 0.5f).toInt()

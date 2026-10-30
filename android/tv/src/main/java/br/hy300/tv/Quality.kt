package br.hy300.tv

/**
 * Canais pesados demais para o projetor (entrega 720p).
 *
 * SD e HD ficam: SD é leve e HD é o teto do aparelho. O que atrapalha é 4K/8K,
 * que exige bitrate alto, esquenta o decoder e costuma travar. Esses são removidos.
 */
object Quality {

    /** 4K, 8K, UHD e as resoluções equivalentes em número. */
    private val tooHeavy = Regex("(?i)\\b(uhd|4k|8k|2160p?|4320p?)\\b")

    fun isTooHeavy(c: Channel): Boolean {
        val text = c.guideName + " " + c.name + " " + c.group
        return tooHeavy.containsMatchIn(text)
    }
}

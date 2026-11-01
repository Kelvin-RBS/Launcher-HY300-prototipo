package br.hy300.games

import java.util.Locale

/**
 * Medições para descobrir de onde vem o atraso dos jogos e o relatório com as conclusões.
 * Kotlin puro (sem Android): quem mede é o EmulatorActivity, aqui só se guarda e se interpreta.
 *
 * O que ele mede: entrega do controle ao jogo, tempo de emulação, ritmo dos quadros, espera por
 * buffer de vídeo e atraso real do áudio. O que NÃO mede: o rádio Bluetooth do controle e o
 * processamento de imagem do projetor (o relatório avisa).
 */
class LatencyProbe {
    class Stat {
        var n = 0L
            private set
        var max = 0.0
            private set
        private var sum = 0.0
        fun add(v: Double) {
            n++
            sum += v
            if (v > max) max = v
        }
        fun avg(): Double = if (n == 0L) 0.0 else sum / n
        fun reset() {
            n = 0
            sum = 0.0
            max = 0.0
        }
    }

    private val lock = Any()

    // ---- medições contínuas ----
    val emu = Stat()          // processamento do núcleo por quadro, sem a espera de vídeo (ms)
    val period = Stat()       // intervalo entre o início de quadros seguidos (ms)
    val keyAge = Stat()       // botão (tecla): do evento até o jogo ler = entrega + espera (ms)
    val moveAge = Stat()      // direcional/analógico (movimento): idem (ms)
    val keyDeliver = Stat()   // botão: do evento até chegar ao app (ms)
    val keyWait = Stat()      // botão: do app até o jogo ler no início do próximo quadro (ms)
    val moveDeliver = Stat()
    val moveWait = Stat()
    val padInterval = Stat()  // intervalo entre relatórios do controle (ms)
    val videoWait = Stat()    // espera média por buffer de vídeo, uma amostra por segundo (ms)
    val audioLat = Stat()     // atraso real do áudio, uma amostra por segundo (ms)
    var lateFrames = 0L       // quadros que demoraram mais de 25 ms
        private set
    var videoWaitMax = 0.0
        private set
    var videoDropped = 0L
        private set
    var underruns = 0
    var fps = 0
    var bufferMs = 0

    // ---- informações fixas (preenchidas uma vez) ----
    @Volatile var outputName: String = ""
    @Volatile var outputType: Int = -1          // AudioDeviceInfo.TYPE_*, -1 = desconhecido
    @Volatile var fastTrack: Boolean? = null    // null = não deu para saber
    @Volatile var nativeRate: Int = 0
    @Volatile var burstFrames: Int = 0          // quadros por rajada de áudio do sistema
    @Volatile var lowLatencyFeature: Boolean = false
    @Volatile var displayHz: Float = 0f
    @Volatile var coreFps: Double = 0.0
    @Volatile var videoTrim: Boolean = false

    // ---- entrada pendente (escrita pela thread principal, lida pela do emulador) ----
    @Volatile private var pendingDispatch = 0L   // quando o app recebeu o aperto (0 = nada pendente)
    @Volatile private var pendingDeliver = 0L    // quanto o aperto levou do evento até o app
    @Volatile private var pendingMotion = false
    private var lastReport = 0L
    private var lastStartNs = 0L

    /**
     * Um botão ou direção acabou de ser apertado. [eventTime] é o horário do evento e [nowMs] o
     * horário em que o app o recebeu (os dois em uptimeMillis).
     */
    fun notePress(eventTime: Long, nowMs: Long, fromMotion: Boolean) {
        if (pendingDispatch == 0L) {
            pendingMotion = fromMotion
            pendingDeliver = (nowMs - eventTime).coerceAtLeast(0L)
            pendingDispatch = nowMs  // por último: é o sinal de que há algo pendente
        }
    }

    /** O jogo vai ler a entrada agora: mede a entrega ao app e a espera pelo quadro. [nowMs] em uptimeMillis. */
    fun consumePress(nowMs: Long) {
        val d = pendingDispatch
        if (d == 0L) return
        val motion = pendingMotion
        val deliver = pendingDeliver.toDouble()
        pendingDispatch = 0L
        val wait = (nowMs - d).coerceAtLeast(0L).toDouble()
        synchronized(lock) {
            if (motion) {
                moveDeliver.add(deliver); moveWait.add(wait); moveAge.add(deliver + wait)
            } else {
                keyDeliver.add(deliver); keyWait.add(wait); keyAge.add(deliver + wait)
            }
        }
    }

    /** Um relatório do controle (eventos do analógico, inclusive os agrupados pelo Android). */
    fun noteReport(eventTime: Long) {
        synchronized(lock) {
            val last = lastReport
            lastReport = eventTime
            if (last != 0L) {
                val d = eventTime - last
                if (d in 1..150) padInterval.add(d.toDouble())
            }
        }
    }

    /** Início de um quadro (System.nanoTime). */
    fun frameStart(nowNs: Long) {
        val last = lastStartNs
        lastStartNs = nowNs
        if (last == 0L) return
        val p = (nowNs - last) / 1_000_000.0
        if (p < 200.0) {
            synchronized(lock) {
                period.add(p)
                if (p > 25.0) lateFrames++
            }
        }
    }

    /** Depois de uma pausa (menu), o primeiro intervalo não vale. */
    fun resetFramePeriod() {
        lastStartNs = 0L
    }

    /** Zera as medições (as informações fixas ficam). */
    fun reset() {
        synchronized(lock) {
            for (st in listOf(emu, period, keyAge, moveAge, keyDeliver, keyWait, moveDeliver, moveWait, padInterval, videoWait, audioLat)) st.reset()
            lateFrames = 0
            videoWaitMax = 0.0
        }
        resetFramePeriod()
    }

    fun addEmu(ms: Double) {
        synchronized(lock) { emu.add(ms) }
    }

    fun addVideo(waitAvgMs: Double, waitMaxMs: Double, dropped: Long, frames: Long) {
        synchronized(lock) {
            if (frames > 0) videoWait.add(waitAvgMs)
            if (waitMaxMs > videoWaitMax) videoWaitMax = waitMaxMs
            videoDropped = dropped
        }
    }

    fun addAudioLatency(ms: Double) {
        synchronized(lock) { audioLat.add(ms) }
    }

    // ------------------------------------------------------------------ relatório

    private fun f(v: Double, digits: Int = 1): String = String.format(Locale("pt", "BR"), "%." + digits + "f", v)

    private fun outputLabel(): String {
        val kind = when (outputType) {
            1, 2 -> "alto-falante do aparelho"
            3, 4 -> "fone com fio"
            5, 6 -> "linha"
            7 -> "Bluetooth (chamada)"
            8 -> "Bluetooth (A2DP)"
            9, 10, 29 -> "HDMI"
            11, 12, 22 -> "USB"
            else -> "desconhecida"
        }
        return if (outputName.isNotEmpty() && outputName != "null") "$kind ($outputName)" else kind
    }

    private fun isBluetoothAudio() = outputType == 7 || outputType == 8

    /** Atraso estimado do aparelho e do app, de ponta a ponta, sem o rádio do controle nem o projetor. */
    private fun vsyncMs(): Double = if (displayHz > 20f) 1000.0 / displayHz else 16.7

    private fun imageEstimate(age: Stat): Double {
        var v = age.avg() + emu.avg() + 1.5 * vsyncMs()
        if (videoWait.avg() > 4.0) v += vsyncMs()  // fila de vídeo cheia: mais um quadro
        return v
    }

    private fun soundEstimate(age: Stat): Double {
        val audio = if (audioLat.n > 0) audioLat.avg() else bufferMs.toDouble()
        return age.avg() + emu.avg() + audio
    }

    fun conclusions(): List<String> {
        val out = ArrayList<String>()
        val frameBudget = 1000.0 / (if (coreFps > 0) coreFps else 60.0)
        val few = emu.n < 600
        val btAudio = isBluetoothAudio()
        if (few) out.add("Poucas medições: jogue pelo menos 30 segundos, apertando botões e mexendo no direcional, e abra o diagnóstico de novo.")

        if (btAudio) {
            out.add("O som sai por Bluetooth, que costuma ter de 100 a 300 ms de atraso fixo (aqui: " + (if (audioLat.n > 0) f(audioLat.avg(), 0) + " ms" else "sem medição") + "). Para jogar, use o HDMI, o alto-falante do aparelho ou um fone com fio (Configurações > Saída de áudio).")
            if (padInterval.n >= 20 && padInterval.avg() > 15.0) {
                out.add("O som e o controle usam Bluetooth ao mesmo tempo e dividem o mesmo rádio, o que costuma espaçar os envios do controle (hoje " + f(padInterval.avg(), 0) + " ms entre envios; o normal é perto de 8 ms) e causar falhas no som. Tire o som do Bluetooth e meça de novo.")
            }
        } else {
            if (fastTrack == false) {
                out.add("O Android não liberou o caminho rápido de áudio (baixa latência): o som tem um atraso mínimo maior do que o ideal. Não há ajuste no app para isso; o atraso real do áudio aparece acima.")
            }
            if (audioLat.n > 0 && audioLat.avg() > 70.0) {
                out.add("O áudio está com " + f(audioLat.avg(), 0) + " ms de atraso. Deixe a latência do áudio em Baixa nos Ajustes do emulador.")
            }
            if (padInterval.n >= 20 && padInterval.avg() > 15.0) {
                out.add("O controle envia um relatório a cada " + f(padInterval.avg()) + " ms (o ideal é perto de 8 ms). Isso é o controle ou o Bluetooth: teste com um controle com fio ou com adaptador USB.")
            }
        }
        if (underruns > 0) {
            out.add("O som falhou " + underruns + " vez(es); o app aumentou o buffer sozinho (hoje " + bufferMs + " ms), o que soma atraso. Falhas seguidas indicam processador no limite, travadas do sistema ou saída de áudio instável.")
        }
        if (emu.n > 0 && (emu.avg() > frameBudget * 0.75 || emu.max > frameBudget * 1.2)) {
            out.add("O processamento do núcleo leva em média " + f(emu.avg()) + " ms por quadro e no máximo " + f(emu.max) + " ms, e o orçamento é de " + f(frameBudget) + " ms: o processador está no limite (ou foi interrompido por outro processo). Isso causa atraso e travadas.")
        }
        if (videoWaitMax > 50.0) {
            out.add("Houve esperas longas por vídeo (até " + f(videoWaitMax, 0) + " ms): o sistema de tela demorou a liberar um buffer. Isso é do Android ou do aparelho (não do processamento do jogo) e explica boa parte dos quadros atrasados.")
        }
        if (period.n > 0 && (period.max > 40.0 || lateFrames > period.n / 100 + 2)) {
            out.add("Houve " + lateFrames + " quadro(s) atrasados (o pior com " + f(period.max, 0) + " ms).")
        }
        if (keyDeliver.n >= 3 && keyDeliver.avg() > 6.0) {
            out.add("Os botões levam " + f(keyDeliver.avg()) + " ms só para chegar ao app (o normal é até uns 4 ms): o sistema ou a thread principal do app estão lentos.")
        }
        if (moveDeliver.n >= 3 && moveDeliver.avg() > keyDeliver.avg() + 6.0) {
            out.add("O direcional chega como \"movimento\" e leva " + f(moveDeliver.avg()) + " ms para chegar ao app (os botões, " + f(keyDeliver.avg()) + " ms). O Android junta esses eventos e os entrega só a cada quadro de tela; no Android 11 o app não consegue evitar isso. Controles cujo direcional é enviado como botões não têm esse atraso.")
        }
        if (videoWait.n > 0 && videoWait.avg() > 4.0) {
            out.add("A fila de vídeo está cheia (o app espera " + f(videoWait.avg()) + " ms por buffer a cada quadro), o que soma 1 ou 2 quadros de atraso na imagem" + (if (videoTrim) "; mesmo com a fila curta ligada" else ". Ligue \"Fila de vídeo curta\" nos Ajustes do emulador e meça de novo") + ".")
        }
        if (videoTrim && videoDropped > 0) {
            out.add("A fila de vídeo curta descartou " + videoDropped + " quadro(s) até agora (no máximo 1 a cada 30).")
        }
        if (displayHz > 0f && Math.abs(displayHz - 60f) > 1.0f) {
            out.add("A tela está em " + f(displayHz.toDouble()) + " Hz, não em 60: o jogo roda a " + f(coreFps) + " quadros por segundo e a imagem pode ter pequenas travadas periódicas.")
        }
        if (!few && out.none { !it.startsWith("A fila de vídeo curta") }) {
            out.add("Do lado do aparelho e do app está tudo dentro do esperado. O atraso que ainda sobrar vem do controle Bluetooth e/ou do projetor: desligue efeitos de imagem (modo jogo, redução de ruído, interpolação) e compare com um controle com fio.")
        }
        return out
    }

    fun report(title: String, stamp: String): String {
        val sb = StringBuilder()
        synchronized(lock) {
            sb.append("DIAGNÓSTICO DE ATRASO\n")
            sb.append(title).append("  ·  ").append(stamp).append("\n")
            sb.append("Medido em ").append(emu.n).append(" quadros (").append(f(emu.n / (if (coreFps > 0) coreFps else 60.0), 0)).append(" s de jogo)\n\n")

            sb.append("ÁUDIO\n")
            sb.append("  Saída: ").append(outputLabel()).append("\n")
            sb.append("  Caminho rápido: ").append(when (fastTrack) { true -> "sim"; false -> "NÃO"; null -> "não foi possível saber" })
            sb.append("  ·  taxa ").append(nativeRate).append(" Hz")
            if (burstFrames > 0 && nativeRate > 0) sb.append("  ·  rajada ").append(f(burstFrames * 1000.0 / nativeRate)).append(" ms")
            sb.append("\n")
            sb.append("  Buffer: ").append(bufferMs).append(" ms")
            if (audioLat.n > 0) sb.append("  ·  atraso real do som: ").append(f(audioLat.avg(), 0)).append(" ms (máx ").append(f(audioLat.max, 0)).append(")")
            sb.append("  ·  falhas ").append(underruns).append("\n\n")

            sb.append("EMULAÇÃO E IMAGEM\n")
            sb.append("  Processamento do núcleo por quadro: média ").append(f(emu.avg())).append(" ms, máx ").append(f(emu.max)).append(" ms (limite ").append(f(1000.0 / (if (coreFps > 0) coreFps else 60.0))).append(" ms)\n")
            sb.append("  Intervalo entre quadros: média ").append(f(period.avg(), 2)).append(" ms, máx ").append(f(period.max)).append(" ms, atrasados ").append(lateFrames).append("\n")
            sb.append("  Espera por buffer de vídeo: média ").append(f(videoWait.avg())).append(" ms, máx ").append(f(videoWaitMax)).append(" ms\n")
            sb.append("  Tela: ").append(if (displayHz > 0f) f(displayHz.toDouble()) else "?").append(" Hz  ·  fila de vídeo curta: ").append(if (videoTrim) "ligada" else "desligada").append("\n\n")

            sb.append("CONTROLE (até chegar ao jogo)\n")
            sb.append("  Botões (").append(keyAge.n).append(" apertos): chegada ao app ").append(f(keyDeliver.avg())).append(" ms + espera pelo quadro ").append(f(keyWait.avg())).append(" ms = ").append(f(keyAge.avg())).append(" ms (máx ").append(f(keyAge.max)).append(")\n")
            sb.append("  Direcional (").append(moveAge.n).append(" movimentos): chegada ao app ").append(f(moveDeliver.avg())).append(" ms + espera pelo quadro ").append(f(moveWait.avg())).append(" ms = ").append(f(moveAge.avg())).append(" ms (máx ").append(f(moveAge.max)).append(")\n")
            sb.append("  (a espera pelo quadro é normal: em média meio quadro, uns 8 ms)\n")
            sb.append("  Intervalo entre envios do controle: ")
            if (padInterval.n >= 5) sb.append("média ").append(f(padInterval.avg())).append(" ms, máx ").append(f(padInterval.max)).append(" ms\n") else sb.append("sem dados (mexa o analógico durante o jogo)\n")
            sb.append("\n")

            sb.append("ATRASO ESTIMADO (aperto até o resultado, sem o rádio do controle e sem o projetor)\n")
            if (keyAge.n > 0) sb.append("  Botão → imagem: ≈ ").append(f(imageEstimate(keyAge), 0)).append(" ms  ·  botão → som: ≈ ").append(f(soundEstimate(keyAge), 0)).append(" ms\n")
            if (moveAge.n > 0) sb.append("  Direcional → imagem: ≈ ").append(f(imageEstimate(moveAge), 0)).append(" ms  ·  direcional → som: ≈ ").append(f(soundEstimate(moveAge), 0)).append(" ms\n")
            if (keyAge.n == 0L && moveAge.n == 0L) sb.append("  sem apertos medidos ainda\n")
            sb.append("  (a imagem inclui cerca de 1,5 quadro de tela; o Bluetooth do controle e o processamento do projetor ficam de fora)\n\n")
        }
        sb.append("CONCLUSÕES\n")
        for (c in conclusions()) sb.append("  • ").append(c).append("\n")
        return sb.toString()
    }

    /** Linha curta para a tela durante o jogo. */
    fun overlayLine(): String {
        synchronized(lock) {
            return "entrada: botões " + f(keyAge.avg()) + " ms · direcional " + f(moveAge.avg()) + " ms · vídeo espera " + f(videoWait.avg()) + " ms"
        }
    }
}

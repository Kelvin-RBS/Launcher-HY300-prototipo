package br.hy300.games

import android.view.Surface

/** Ponte (JNI) para o motor em C++ (cpp/emu.cpp). Só funciona se a parte nativa foi compilada. */
object NativeEmu {
    @JvmField
    var ready = false

    init {
        ready = try {
            System.loadLibrary("hy300emu")
            true
        } catch (e: UnsatisfiedLinkError) {
            false
        }
    }

    external fun loadCore(path: String, dir: String): Boolean
    external fun lastError(): String
    /** Últimas mensagens de erro/aviso do núcleo (explicam, por exemplo, por que ele recusou um jogo). */
    external fun lastLog(): String
    /** [espera média por buffer de vídeo (ms), máxima, tempo médio do quadro de vídeo, máximo, quadros, descartados]; zera a contagem. */
    external fun videoStats(): DoubleArray
    /** 1 = descarta um quadro quando a fila de vídeo enche (menos atraso de imagem; experimental). */
    external fun setVideoTrim(on: Int)
    external fun loadGame(data: ByteArray, name: String): Boolean
    /** [fps, taxa de áudio, largura, altura, proporção] */
    external fun avInfo(): DoubleArray
    /** Taxa do núcleo e taxa de saída do aparelho: o motor converte o áudio entre as duas. */
    external fun setAudioRates(inRate: Double, outRate: Double)
    external fun setSurface(surface: Surface?)
    /** Roda um quadro. Devolve quantas amostras de áudio (estéreo intercalado) foram escritas em [audio]. */
    external fun runFrame(audio: ShortArray): Int
    external fun setInput(port: Int, mask: Int)
    external fun serializeState(): ByteArray?
    external fun unserializeState(data: ByteArray): Boolean
    external fun sram(): ByteArray?
    external fun setSram(data: ByteArray)
    external fun reset()
    external fun unload()
}

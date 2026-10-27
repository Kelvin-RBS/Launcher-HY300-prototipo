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
    external fun loadGame(data: ByteArray, name: String): Boolean
    /** [fps, taxa de áudio, largura, altura, proporção] */
    external fun avInfo(): DoubleArray
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

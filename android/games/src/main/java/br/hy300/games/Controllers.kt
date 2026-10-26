package br.hy300.games

import android.view.InputDevice

object Controllers {
    /** Controles de jogo conectados (Bluetooth ou USB): Xbox, Nintendo, PlayStation, genéricos... */
    fun connected(): List<InputDevice> {
        val result = ArrayList<InputDevice>()
        for (id in InputDevice.getDeviceIds()) {
            val device = InputDevice.getDevice(id) ?: continue
            if (device.isVirtual) continue
            val s = device.sources
            val gamepad = (s and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD
            val joystick = (s and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK
            if (gamepad || joystick) result.add(device)
        }
        return result
    }
}

package com.houvven.guise.xposed.hook

import android.media.AudioDeviceInfo
import android.media.AudioManager
import com.houvven.guise.xposed.LoadPackageHandler
import com.houvven.ktx_xposed.hook.afterHookAllMethods
import com.houvven.ktx_xposed.hook.afterHookedMethod

class AudioDeviceHook : LoadPackageHandler {
    override fun onHook() {
        AudioManager::class.java.afterHookedMethod(
            "getDevices",
            Int::class.javaPrimitiveType!!,
        ) { param ->
            val devices = param.result as? Array<*> ?: return@afterHookedMethod
            param.result = devices.filterIsInstance<AudioDeviceInfo>()
                .filterNot { isExternalAudioDeviceType(it.type) }
                .toTypedArray()
        }
        AudioManager::class.java.afterHookAllMethods("getDevicesForAttributes") { param ->
            val devices = param.result as? List<*> ?: return@afterHookAllMethods
            param.result = devices.filterIsInstance<AudioDeviceInfo>()
                .filterNot { isExternalAudioDeviceType(it.type) }
        }
    }
}

internal fun isExternalAudioDeviceType(type: Int): Boolean = type in EXTERNAL_AUDIO_TYPES

private val EXTERNAL_AUDIO_TYPES = setOf(
    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
    AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
    AudioDeviceInfo.TYPE_USB_ACCESSORY,
    AudioDeviceInfo.TYPE_USB_DEVICE,
    AudioDeviceInfo.TYPE_USB_HEADSET,
    AudioDeviceInfo.TYPE_HDMI,
    AudioDeviceInfo.TYPE_HDMI_ARC,
    AudioDeviceInfo.TYPE_HDMI_EARC,
    AudioDeviceInfo.TYPE_DOCK,
    AudioDeviceInfo.TYPE_LINE_ANALOG,
    AudioDeviceInfo.TYPE_LINE_DIGITAL,
    AudioDeviceInfo.TYPE_IP,
    AudioDeviceInfo.TYPE_BUS,
    AudioDeviceInfo.TYPE_HEARING_AID,
    AudioDeviceInfo.TYPE_BLE_HEADSET,
    AudioDeviceInfo.TYPE_BLE_SPEAKER,
    AudioDeviceInfo.TYPE_BLE_BROADCAST,
)

package com.houvven.guise.xposed.hook.netowork

import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import com.houvven.guise.xposed.LoadPackageHandler
import com.houvven.guise.xposed.config.HooksValue
import com.houvven.ktx_xposed.hook.afterHookedMethod
import com.houvven.ktx_xposed.hook.setMethodResult

internal class WifiHook : LoadPackageHandler {
    override fun onHook() {
        // LocationHook owns these values when Wi-Fi-derived identifiers are intentionally hidden.
        if (config.makeWifiLocationFail) return
        WifiInfo::class.java.run {
            if (config.wifiSSID.isNotBlank()) setMethodResult("getSSID", "\"${config.wifiSSID}\"")
            if (config.wifiBSSID.isNotBlank()) setMethodResult("getBSSID", config.wifiBSSID)
            if (config.wifiMacAddress.isNotBlank()) setMethodResult("getMacAddress", config.wifiMacAddress)
        }

        // Preserve the original Guise behavior: mutate the returned object before application-side
        // privacy wrappers can cache or copy it. Getter hooks above remain as a compatibility layer.
        if (config.wifiBSSID.isNotBlank() || config.wifiMacAddress.isNotBlank()) {
            WifiManager::class.java.afterHookedMethod("getConnectionInfo") { param ->
                param.result?.let { wifiInfo ->
                    if (config.wifiBSSID.isNotBlank()) {
                        setWifiInfoField(wifiInfo, "mBSSID", config.wifiBSSID)
                    }
                    if (config.wifiMacAddress.isNotBlank()) {
                        setWifiInfoField(wifiInfo, "mMacAddress", config.wifiMacAddress)
                    }
                }
            }
        }

        if (config.networkType == HooksValue.NET_WIFI) {
            WifiManager::class.java.setMethodResult("getWifiState", WifiManager.WIFI_STATE_ENABLED)
        }
    }

    private fun setWifiInfoField(wifiInfo: Any, name: String, value: String) {
        runCatching {
            wifiInfo.javaClass.getDeclaredField(name).apply { isAccessible = true }.set(wifiInfo, value)
        }
    }
}

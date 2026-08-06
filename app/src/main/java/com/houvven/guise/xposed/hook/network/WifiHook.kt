package com.houvven.guise.xposed.hook.network

import android.net.wifi.WifiInfo
import com.houvven.guise.xposed.LoadPackageHandler
import com.houvven.ktx_xposed.hook.setMethodResult

internal class WifiHook : LoadPackageHandler {
    override fun onHook() {
        if (config.makeWifiLocationFail) return
        WifiInfo::class.java.run {
            if (config.wifiSSID.isNotBlank()) setMethodResult("getSSID", "\"${config.wifiSSID}\"")
            if (config.wifiBSSID.isNotBlank()) setMethodResult("getBSSID", config.wifiBSSID)
            if (config.wifiMacAddress.isNotBlank()) setMethodResult("getMacAddress", config.wifiMacAddress)
        }
    }
}

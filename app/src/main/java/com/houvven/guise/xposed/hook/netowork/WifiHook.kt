package com.houvven.guise.xposed.hook.netowork

import android.net.wifi.ScanResult
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import com.houvven.guise.xposed.LoadPackageHandler
import com.houvven.guise.xposed.config.HooksValue
import com.houvven.ktx_xposed.hook.afterHookedMethod
import com.houvven.ktx_xposed.hook.setMethodResult

internal class WifiHook : LoadPackageHandler {
    @Volatile
    private var connectedBssid: String? = null

    override fun onHook() {
        // LocationHook owns these values when Wi-Fi-derived identifiers are intentionally hidden.
        if (config.makeWifiLocationFail) return
        if (config.wifiSSID.isNotBlank() || config.wifiBSSID.isNotBlank()) {
            hookConnectedBssid()
            hookConnectedScanResult()
        }

        WifiInfo::class.java.run {
            if (config.wifiSSID.isNotBlank()) setMethodResult("getSSID", "\"${config.wifiSSID}\"")
            if (config.wifiMacAddress.isNotBlank()) setMethodResult("getMacAddress", config.wifiMacAddress)
        }

        if (config.networkType == HooksValue.NET_WIFI) {
            WifiManager::class.java.run {
                setMethodResult("getWifiState", WifiManager.WIFI_STATE_ENABLED)
                setMethodResult("isWifiEnabled", true)
            }
        }
    }

    private fun hookConnectedBssid() {
        WifiInfo::class.java.afterHookedMethod("getBSSID") { param ->
            connectedBssid = param.result as? String
            if (config.wifiBSSID.isNotBlank()) param.result = config.wifiBSSID
        }
    }

    private fun hookConnectedScanResult() {
        WifiManager::class.java.afterHookedMethod("getScanResults") { param ->
            val wifiManager = param.thisObject as? WifiManager ?: return@afterHookedMethod
            val scanResults = param.result as? List<*> ?: return@afterHookedMethod
            if (scanResults.isEmpty()) return@afterHookedMethod
            wifiManager.connectionInfo?.bssid ?: return@afterHookedMethod
            val currentBssid = connectedBssid ?: return@afterHookedMethod

            scanResults.asSequence()
                .filterIsInstance<ScanResult>()
                .filter { currentBssid.equals(it.BSSID, ignoreCase = true) }
                .forEach { result ->
                    if (config.wifiSSID.isNotBlank()) result.SSID = config.wifiSSID
                    if (config.wifiBSSID.isNotBlank()) result.BSSID = config.wifiBSSID
                }
        }
    }
}

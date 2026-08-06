@file:Suppress("DEPRECATION")

package com.houvven.guise.xposed.hook.network

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkInfo
import com.houvven.guise.constant.NetworkType
import com.houvven.guise.xposed.LoadPackageHandler
import com.houvven.guise.xposed.config.HooksValue
import com.houvven.ktx_xposed.hook.beforeHookedMethod
import com.houvven.ktx_xposed.hook.setMethodResult

internal class NetworkHook : LoadPackageHandler {

    override fun onHook() {
        if (config.networkType != HooksValue.NET_UNHOOK) hookNetworkType()
        listOf(WifiHook(), SimHook()).forEach { it.onHook() }
    }

    private fun hookNetworkType() {
        val networkType = config.networkType
        if (networkType == HooksValue.NET_NONE) hideActiveNetwork()
        hookBaseNetType(networkType)
        if (networkType != HooksValue.NET_WIFI) {
            SimHook().hookMobileType(networkType)
        }
    }

    private fun hookBaseNetType(type: Int) {
        val baseType = when (type) {
            HooksValue.NET_WIFI -> NetworkType.WIFI
            HooksValue.NET_MOBILE_5G,
            HooksValue.NET_MOBILE_4G,
            HooksValue.NET_MOBILE_3G,
            HooksValue.NET_MOBILE_2G -> NetworkType.MOBILE
            else -> NetworkType.NONE
        }
        NetworkInfo::class.java.setMethodResult("getType", baseType)
        NetworkInfo::class.java.setMethodResult(
            "getTypeName",
            when (baseType) {
                NetworkType.WIFI -> "WIFI"
                NetworkType.MOBILE -> "MOBILE"
                else -> "NONE"
            },
        )

        val telephonyType = SimHook.mobileTelephonyType(type)
        NetworkInfo::class.java.setMethodResult("getSubtype", telephonyType)
        NetworkInfo::class.java.setMethodResult(
            "getSubtypeName",
            telephonySubtypeName(telephonyType),
        )

        NetworkCapabilities::class.java.beforeHookedMethod(
            "hasTransport",
            Int::class.javaPrimitiveType!!,
        ) { param ->
            when (param.args.firstOrNull() as? Int) {
                NetworkCapabilities.TRANSPORT_WIFI -> param.result = baseType == NetworkType.WIFI
                NetworkCapabilities.TRANSPORT_CELLULAR -> param.result = baseType == NetworkType.MOBILE
            }
        }
    }

    private fun hideActiveNetwork() {
        ConnectivityManager::class.java.run {
            setMethodResult("getActiveNetworkInfo", null)
            setMethodResult("getActiveNetwork", null)
            setMethodResult("getAllNetworks", emptyArray<Network>())
        }
    }

    private fun telephonySubtypeName(type: Int): String = when (type) {
        android.telephony.TelephonyManager.NETWORK_TYPE_CDMA -> "CDMA"
        android.telephony.TelephonyManager.NETWORK_TYPE_TD_SCDMA -> "TD-SCDMA"
        android.telephony.TelephonyManager.NETWORK_TYPE_LTE -> "LTE"
        android.telephony.TelephonyManager.NETWORK_TYPE_NR -> "NR"
        else -> ""
    }
}

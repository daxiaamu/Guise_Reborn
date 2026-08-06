@file:Suppress("DEPRECATION")

package com.houvven.guise.xposed.hook.network

import android.telephony.CellIdentityCdma
import android.telephony.CellIdentityGsm
import android.telephony.CellIdentityLte
import android.telephony.CellIdentityNr
import android.telephony.CellIdentityTdscdma
import android.telephony.CellIdentityWcdma
import android.telephony.SubscriptionInfo
import android.telephony.TelephonyManager
import com.houvven.guise.xposed.LoadPackageHandler
import com.houvven.guise.xposed.config.HooksValue
import com.houvven.ktx_xposed.hook.findMethodExactIfExists
import com.houvven.ktx_xposed.hook.setMethodResult
import com.houvven.ktx_xposed.hook.setSomeSameNameMethodResult
import com.houvven.ktx_xposed.hook.setSomeSameNameMethodResultForAnyClass

internal class SimHook : LoadPackageHandler {
    override fun onHook() {
        if (config.simOperator.isNotBlank()) hookSimOperator()
        if (config.simOperatorName.isNotBlank()) hookSimOperatorName()
        if (config.simCountry.isNotBlank()) hookSimCountryIso()
    }

    internal fun hookMobileType(networkType: Int) {
        val type = mobileTelephonyType(networkType)
        TelephonyManager::class.java.setSomeSameNameMethodResult(
            "getNetworkType",
            "getDataNetworkType",
            "getVoiceNetworkType",
            value = type,
        )
    }

    companion object {
        internal fun mobileTelephonyType(networkType: Int): Int = when (networkType) {
            HooksValue.NET_MOBILE_2G -> TelephonyManager.NETWORK_TYPE_CDMA
            HooksValue.NET_MOBILE_3G -> TelephonyManager.NETWORK_TYPE_TD_SCDMA
            HooksValue.NET_MOBILE_4G -> TelephonyManager.NETWORK_TYPE_LTE
            HooksValue.NET_MOBILE_5G -> TelephonyManager.NETWORK_TYPE_NR
            else -> TelephonyManager.NETWORK_TYPE_UNKNOWN
        }
    }

    private fun hookSimOperator() {
        val simOperator = config.simOperator
        if (simOperator.length !in 5..6 || simOperator.any { !it.isDigit() }) return
        val mcc = simOperator.take(3)
        val mnc = simOperator.drop(3)
        val mccInt = mcc.toInt()
        val mncInt = mnc.toInt()

        TelephonyManager::class.java.setSomeSameNameMethodResult(
            "getSimOperatorNumericForPhone",
            "getNetworkOperatorForPhone",
            "getSimOperator",
            "getNetworkOperator",
            value = simOperator,
        )

        arrayOf(
            SubscriptionInfo::class.java,
            CellIdentityCdma::class.java,
            CellIdentityGsm::class.java,
            CellIdentityLte::class.java,
            CellIdentityNr::class.java,
            CellIdentityTdscdma::class.java,
            CellIdentityWcdma::class.java,
        ).forEach { identityClass ->
            identityClass.run {
                findMethodExactIfExists("getMcc")?.setMethodResult(mccInt)
                findMethodExactIfExists("getMnc")?.setMethodResult(mncInt)
                findMethodExactIfExists("getMccString")?.setMethodResult(mcc)
                findMethodExactIfExists("getMncString")?.setMethodResult(mnc)
            }
        }
    }

    private fun hookSimOperatorName() {
        setSomeSameNameMethodResultForAnyClass(
            listOf(
                TelephonyManager::class.java to "getSimOperatorName",
                TelephonyManager::class.java to "getSimOperatorNameForPhone",
                TelephonyManager::class.java to "getNetworkOperatorName",
                TelephonyManager::class.java to "getNetworkOperatorNameForPhone",
                SubscriptionInfo::class.java to "getCarrierName",
                SubscriptionInfo::class.java to "getDisplayName",
            ),
            value = config.simOperatorName,
        )
    }

    private fun hookSimCountryIso() {
        setSomeSameNameMethodResultForAnyClass(
            listOf(
                TelephonyManager::class.java to "getSimCountryIso",
                TelephonyManager::class.java to "getSimCountryIsoForPhone",
                TelephonyManager::class.java to "getNetworkCountryIso",
                TelephonyManager::class.java to "getNetworkCountryIsoForPhone",
                SubscriptionInfo::class.java to "getCountryIso",
            ),
            value = config.simCountry,
        )
    }
}

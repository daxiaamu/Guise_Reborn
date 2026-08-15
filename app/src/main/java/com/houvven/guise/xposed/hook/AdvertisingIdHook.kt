package com.houvven.guise.xposed.hook

import com.houvven.guise.xposed.LoadPackageHandler
import com.houvven.ktx_xposed.hook.findClassIfExists
import com.houvven.ktx_xposed.hook.setAllMethodResult

class AdvertisingIdHook : LoadPackageHandler {
    override fun onHook() {
        val advertisingId = config.advertisingId
        if (advertisingId.isBlank()) return

        findClassIfExists("com.google.android.gms.ads.identifier.AdvertisingIdClient\$Info")
            ?.setAllMethodResult("getId", advertisingId)
        findClassIfExists("android.adservices.adid.AdId")
            ?.setAllMethodResult("getAdId", advertisingId)
    }
}

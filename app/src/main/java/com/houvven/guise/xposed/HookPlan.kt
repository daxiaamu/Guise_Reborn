package com.houvven.guise.xposed

import com.houvven.guise.xposed.config.HooksValue
import com.houvven.guise.xposed.config.ModuleConfig

internal enum class HookFeature {
    BATTERY,
    LOCALE,
    TIME_ZONE,
    LOCATION,
    CELL_LOCATION,
    NETWORK,
    OS_BUILD,
    SYSTEM_PROPERTIES,
    DISPLAY_DENSITY,
    GPU,
    WEBVIEW,
    CAMERA,
    AUDIO_DEVICES,
    SCREENSHOTS,
    UNIQUE_ID,
    ADVERTISING_ID,
    BLANK_PASS,
    APPLICATION_LIST,
    APP_VERSION,
}

internal fun ModuleConfig.activeHookFeatures(): List<HookFeature> = buildList {
    if (batteryLevel != -1) add(HookFeature.BATTERY)
    if (language.isNotBlank()) add(HookFeature.LOCALE)
    if (timeZone.isNotBlank()) add(HookFeature.TIME_ZONE)
    if (
        longitude != -1.0 ||
        latitude != -1.0 ||
        makeWifiLocationFail ||
        makeCellLocationFail
    ) {
        add(HookFeature.LOCATION)
    }
    if ((lac != -1 || cid != -1) && !makeCellLocationFail) add(HookFeature.CELL_LOCATION)
    if (
        networkType != HooksValue.NET_UNHOOK ||
        wifiSSID.isNotBlank() ||
        wifiBSSID.isNotBlank() ||
        wifiMacAddress.isNotBlank() ||
        simOperator.isNotBlank() ||
        simOperatorName.isNotBlank() ||
        simCountry.isNotBlank() ||
        visibleSimCount >= 0
    ) {
        add(HookFeature.NETWORK)
    }

    val hasBuildIdentity =
        brand.isNotBlank() ||
        manufacturer.isNotBlank() ||
        model.isNotBlank() ||
        product.isNotBlank() ||
        device.isNotBlank() ||
        board.isNotBlank() ||
        hardware.isNotBlank() ||
        buildId.isNotBlank() ||
        fingerPrint.isNotBlank() ||
        androidVersion.isNotBlank() ||
        sdkInt != -1
    if (hasBuildIdentity) {
        add(HookFeature.OS_BUILD)
        add(HookFeature.SYSTEM_PROPERTIES)
    }

    if (densityDpi != -1) add(HookFeature.DISPLAY_DENSITY)
    if (gpuVendor.isNotBlank() || gpuRenderer.isNotBlank()) add(HookFeature.GPU)
    if (webViewUserAgent.isNotBlank()) add(HookFeature.WEBVIEW)
    if (cameraCount >= 0) add(HookFeature.CAMERA)
    if (hideExternalAudioDevices) add(HookFeature.AUDIO_DEVICES)
    if (screenshotsFlag != HooksValue.SCREENSHOTS_UNHOOK) add(HookFeature.SCREENSHOTS)
    if (androidId.isNotBlank() || imei.isNotBlank() || phoneNum.isNotBlank()) {
        add(HookFeature.UNIQUE_ID)
    }
    if (advertisingId.isNotBlank()) add(HookFeature.ADVERTISING_ID)
    if (passAudio || passVideo || passPhoto || passContacts) add(HookFeature.BLANK_PASS)
    if (passApplications) add(HookFeature.APPLICATION_LIST)
    if (versionName.isNotBlank() || versionCode != -1) add(HookFeature.APP_VERSION)
}

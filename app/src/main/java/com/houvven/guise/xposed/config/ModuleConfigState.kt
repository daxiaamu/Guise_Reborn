package com.houvven.guise.xposed.config

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf

class ModuleConfigState private constructor(moduleConfig: ModuleConfig) {

    val brand = mutableStateOf(moduleConfig.brand)
    val manufacturer = mutableStateOf(moduleConfig.manufacturer)
    val model = mutableStateOf(moduleConfig.model)
    val product = mutableStateOf(moduleConfig.product)
    val device = mutableStateOf(moduleConfig.device)
    val board = mutableStateOf(moduleConfig.board)
    val hardware = mutableStateOf(moduleConfig.hardware)
    val buildId = mutableStateOf(moduleConfig.buildId)
    val androidVersion = mutableStateOf(moduleConfig.androidVersion)
    val sdkInt = mutableStateOf(moduleConfig.sdkInt.display(-1))
    val densityDpi = mutableStateOf(moduleConfig.densityDpi.display(-1))
    val fingerPrint = mutableStateOf(moduleConfig.fingerPrint)
    val gpuVendor = mutableStateOf(moduleConfig.gpuVendor)
    val gpuRenderer = mutableStateOf(moduleConfig.gpuRenderer)
    val cameraCount = mutableStateOf(moduleConfig.cameraCount.display(-1))

    val networkType = mutableStateOf(moduleConfig.networkType.display(HooksValue.NET_UNHOOK))
    val wifiSSID = mutableStateOf(moduleConfig.wifiSSID)
    val wifiBSSID = mutableStateOf(moduleConfig.wifiBSSID)
    val wifiMacAddress = mutableStateOf(moduleConfig.wifiMacAddress)
    val simOperator = mutableStateOf(moduleConfig.simOperator)
    val simOperatorName = mutableStateOf(moduleConfig.simOperatorName)
    val simCountry = mutableStateOf(moduleConfig.simCountry)
    val visibleSimCount = mutableStateOf(moduleConfig.visibleSimCount.display(-1))

    val imei = mutableStateOf(moduleConfig.imei)
    val phoneNum = mutableStateOf(moduleConfig.phoneNum)
    val androidId = mutableStateOf(moduleConfig.androidId)
    val advertisingId = mutableStateOf(moduleConfig.advertisingId)

    val lac = mutableStateOf(moduleConfig.lac.display(-1))
    val cid = mutableStateOf(moduleConfig.cid.display(-1))

    val longitude = mutableStateOf(moduleConfig.longitude.display(-1.0))
    val latitude = mutableStateOf(moduleConfig.latitude.display(-1.0))
    val randomOffset = mutableStateOf(moduleConfig.randomOffset)
    val makeWifiLocationFail = mutableStateOf(moduleConfig.makeWifiLocationFail)
    val makeCellLocationFail = mutableStateOf(moduleConfig.makeCellLocationFail)

    val versionCode = mutableStateOf(moduleConfig.versionCode.display(-1))
    val versionName = mutableStateOf(moduleConfig.versionName)

    val batteryLevel = mutableStateOf(moduleConfig.batteryLevel.display(-1))
    val language = mutableStateOf(moduleConfig.language)
    val timeZone = mutableStateOf(moduleConfig.timeZone)
    val webViewUserAgent = mutableStateOf(moduleConfig.webViewUserAgent)
    val hideExternalAudioDevices = mutableStateOf(moduleConfig.hideExternalAudioDevices)
    val allowForceScreenshots = mutableStateOf(
        moduleConfig.screenshotsFlag == HooksValue.SCREENSHOTS_ENABLE
    )

    val passContacts = mutableStateOf(moduleConfig.passContacts)
    val passPhoto = mutableStateOf(moduleConfig.passPhoto)
    val passVideo = mutableStateOf(moduleConfig.passVideo)
    val passAudio = mutableStateOf(moduleConfig.passAudio)
    val passApplications = mutableStateOf(moduleConfig.passApplications)

    internal fun clear() {
        stringStates.forEach { it.value = "" }
        booleanStates.forEach { it.value = false }
    }

    private val stringStates: List<MutableState<String>>
        get() = listOf(
            brand, manufacturer, model, product, device, board, hardware, buildId,
            androidVersion, sdkInt, densityDpi, fingerPrint, gpuVendor, gpuRenderer, cameraCount,
            networkType, wifiSSID, wifiBSSID, wifiMacAddress, simOperator, simOperatorName,
            simCountry, visibleSimCount, imei, phoneNum, androidId, advertisingId, lac, cid,
            longitude, latitude, versionCode, versionName, batteryLevel, language, timeZone,
            webViewUserAgent,
        )

    private val booleanStates: List<MutableState<Boolean>>
        get() = listOf(
            randomOffset, makeWifiLocationFail, makeCellLocationFail, hideExternalAudioDevices,
            allowForceScreenshots, passContacts, passPhoto, passVideo, passAudio, passApplications,
        )

    companion object {
        fun of(moduleConfig: ModuleConfig) = ModuleConfigState(moduleConfig)
    }
}

private fun Any.display(default: Any): String = if (this == default) "" else toString()

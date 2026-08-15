package com.houvven.guise.ui.utils

import android.content.Context
import android.hardware.camera2.CameraManager
import android.os.Build
import android.telephony.TelephonyManager
import android.webkit.WebSettings
import com.houvven.guise.db.Device
import com.houvven.guise.db.DeviceDBHelper
import com.houvven.guise.module.preset.CarrierPreset
import com.houvven.guise.module.preset.CarrierPresetRepository
import com.houvven.guise.module.preset.PresetRepository
import com.houvven.guise.module.preset.ResourcePreset
import com.houvven.guise.module.preset.TimeZonePresetRepository
import com.houvven.guise.util.android.Randoms
import com.houvven.guise.xposed.config.HooksValue
import com.houvven.guise.xposed.config.ModuleConfigState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.random.Random

suspend fun oneClickRandom(state: ModuleConfigState, context: Context) {
    val values = withContext(Dispatchers.IO) {
        val presetCatalog = PresetRepository.get(context)
        val network = presetCatalog.networks.randomOrNull()
            ?: error("No network presets are available")
        val carrier = if (network.value.toIntOrNull()?.isMobileNetwork() == true) {
            CarrierPresetRepository.get(context).randomOrNull()
                ?: error("No carrier presets are available")
        } else {
            null
        }
        val (brand, device) = selectRandomDevice(context)
        RandomSelection(
            brand = brand,
            device = device,
            android = selectCompatibleAndroid(presetCatalog.androidVersions),
            densityDpi = presetCatalog.densityDpi.randomOrNull()?.value.orEmpty(),
            language = presetCatalog.languages.randomOrNull()?.value.orEmpty(),
            network = network,
            carrier = carrier,
            actualCameraCount = runCatching {
                context.getSystemService(CameraManager::class.java).cameraIdList.size
            }.getOrNull(),
            maxModems = runCatching {
                val telephony = context.getSystemService(TelephonyManager::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    telephony.activeModemCount
                } else {
                    @Suppress("DEPRECATION")
                    telephony.phoneCount
                }
            }.getOrNull(),
            baseWebViewUserAgent = runCatching {
                WebSettings.getDefaultUserAgent(context)
            }.getOrDefault(""),
        )
    }

    state.run {
        val modelName = values.device.model.orEmpty()
        val deviceCode = values.device.codeAlias?.takeIf(String::isNotBlank)
            ?: values.device.code.orEmpty()
        val normalizedDevice = deviceCode.ifBlank { modelName.fingerprintSafePart() }
        val version = values.android.value.substringBefore('|')
        val api = values.android.value.substringAfter('|')
        val profileApi = api.toIntOrNull() ?: MIN_PROFILE_API
        val generatedBuildId = Randoms.randomBuildId(version)
        val gpu = selectGpuIdentity(values.brand, profileApi)
        val mobileNetwork = values.network.value.toIntOrNull()?.isMobileNetwork() == true

        brand.value = values.brand
        manufacturer.value = values.brand
        model.value = modelName
        device.value = normalizedDevice
        product.value = normalizedDevice
        board.value = ""
        hardware.value = ""
        androidVersion.value = version
        sdkInt.value = api
        densityDpi.value = values.densityDpi
        buildId.value = generatedBuildId
        fingerPrint.value = Randoms.randomFingerprint(
            brand = values.brand,
            product = normalizedDevice,
            device = normalizedDevice,
            androidVersion = version,
            buildId = generatedBuildId,
        )
        gpuVendor.value = gpu.vendor
        gpuRenderer.value = gpu.renderer
        cameraCount.value = selectVisibleCameraCount(
            values.actualCameraCount ?: DEFAULT_CAMERA_COUNT_CAP,
        ).toString()

        networkType.value = values.network.value
        applyNetworkSpecificValues(values.network.value.toIntOrNull(), values.carrier)
        visibleSimCount.value = selectVisibleSimCount(
            values.maxModems ?: DEFAULT_MODEM_COUNT_CAP,
            mobileNetwork = mobileNetwork,
        ).toString()

        Randoms.randomCoordinates().let { (lat, lon) ->
            latitude.value = lat.toString()
            longitude.value = lon.toString()
        }
        randomOffset.value = false
        makeWifiLocationFail.value = false
        makeCellLocationFail.value = false

        androidId.value = Randoms.randomAndroidId()
        imei.value = Randoms.randomIMEI()
        phoneNum.value = Randoms.randomPhoneNum()
        advertisingId.value = Randoms.uuid()

        batteryLevel.value = Randoms.randomBatteryLevel().toString()
        language.value = values.language
        timeZone.value = TimeZonePresetRepository.randomId()
        webViewUserAgent.value = rewriteWebViewUserAgent(
            baseUserAgent = values.baseWebViewUserAgent,
            androidVersion = version,
            model = modelName,
        )
        hideExternalAudioDevices.value = randomizeExternalAudioVisibility()
        versionCode.value = ""
        versionName.value = ""
    }
}

private fun ModuleConfigState.applyNetworkSpecificValues(
    networkType: Int?,
    carrier: CarrierPreset?,
) {
    when {
        networkType == HooksValue.NET_WIFI -> {
            wifiSSID.value = Randoms.randomString(10)
            wifiBSSID.value = Randoms.randomMacAddress()
            wifiMacAddress.value = Randoms.randomMacAddress()
            clearCarrierValues()
            lac.value = ""
            cid.value = ""
        }

        networkType?.isMobileNetwork() == true -> {
            wifiSSID.value = ""
            wifiBSSID.value = ""
            wifiMacAddress.value = ""
            simOperatorName.value = carrier?.name.orEmpty()
            simOperator.value = carrier?.plmn.orEmpty()
            simCountry.value = carrier?.countryCode.orEmpty()
            lac.value = Random.nextInt(1, 65_536).toString()
            cid.value = Random.nextInt(1, 268_435_456).toString()
        }

        else -> {
            wifiSSID.value = ""
            wifiBSSID.value = ""
            wifiMacAddress.value = ""
            clearCarrierValues()
            lac.value = ""
            cid.value = ""
        }
    }
}

private fun ModuleConfigState.clearCarrierValues() {
    simOperatorName.value = ""
    simOperator.value = ""
    simCountry.value = ""
}

private fun selectRandomDevice(context: Context): Pair<String, Device> =
    DeviceDBHelper(context).use { deviceDB ->
        deviceDB.getAllBrand().keys
            .shuffled()
            .firstNotNullOfOrNull { brand ->
                deviceDB.getDevicesByBrand(brand).randomOrNull()?.let { brand to it }
            }
            ?: error("No device profiles are available")
    }

internal fun selectCompatibleAndroid(
    androidVersions: List<ResourcePreset>,
    deviceApi: Int = Build.VERSION.SDK_INT,
): ResourcePreset {
    val supported = androidVersions.filter { preset ->
        preset.value.substringAfter('|').toIntOrNull()?.let { api ->
            api in MIN_PROFILE_API..deviceApi
        } == true
    }
    return supported.randomOrNull()
        ?: error("No Android preset supports API $deviceApi")
}

private fun Int.isMobileNetwork(): Boolean =
    this in HooksValue.NET_MOBILE_2G..HooksValue.NET_MOBILE_5G

private data class RandomSelection(
    val brand: String,
    val device: Device,
    val android: ResourcePreset,
    val densityDpi: String,
    val language: String,
    val network: ResourcePreset,
    val carrier: CarrierPreset?,
    val actualCameraCount: Int?,
    val maxModems: Int?,
    val baseWebViewUserAgent: String,
)

private fun String.fingerprintSafePart(): String =
    trim().replace(Regex("[\\s/:]+"), "_").ifBlank { "device" }

private const val MIN_PROFILE_API = 29
private const val DEFAULT_CAMERA_COUNT_CAP = 4
private const val DEFAULT_MODEM_COUNT_CAP = 2

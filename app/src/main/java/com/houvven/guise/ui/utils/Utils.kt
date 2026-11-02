package com.houvven.guise.ui.utils

import android.content.Context
import com.houvven.guise.device.DeviceCatalogRepository
import com.houvven.guise.module.preset.CarrierPreset
import com.houvven.guise.module.preset.CarrierPresetRepository
import com.houvven.guise.module.preset.PresetRepository
import com.houvven.guise.module.preset.TimeZonePresetRepository
import com.houvven.guise.module.preset.randomValidCarrier
import com.houvven.guise.util.android.Randoms
import com.houvven.guise.xposed.config.ModuleConfigState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun oneClickRandom(state: ModuleConfigState, context: Context) {
    val values = withContext(Dispatchers.IO) {
        val catalog = DeviceCatalogRepository.get(context)
        val brand = catalog.brands.filter { it.models.isNotEmpty() }.random()
        val device = brand.models.random()
        val android = PresetRepository.get(context).androidVersions
            .filter { it.value.substringAfter('|').toIntOrNull()?.let { api -> api >= 29 } == true }
            .random()
        val carrier = CarrierPresetRepository.get(context).randomValidCarrier()
        RandomSelection(
            brand = brand.buildBrand,
            manufacturer = brand.manufacturer,
            model = device.model,
            device = device.resolvedDevice(),
            android = android.value,
            carrier = carrier,
        )
    }

    state.run {
        val fingerprintProduct = product.value.ifBlank {
            values.device.ifBlank { values.model.fingerprintSafePart() }
        }
        val version = values.android.substringBefore('|')
        val api = values.android.substringAfter('|')
        val generatedBuildId = Randoms.randomBuildId(version)

        brand.value = values.brand
        manufacturer.value = values.manufacturer
        model.value = values.model
        device.value = values.device
        androidVersion.value = version
        sdkInt.value = api
        buildId.value = generatedBuildId
        fingerPrint.value = Randoms.randomFingerprint(
            brand = values.brand,
            product = fingerprintProduct,
            device = values.device,
            androidVersion = version,
            buildId = generatedBuildId,
        )

        networkType.value = PresetRepository.get(context).networks.random().value

        wifiSSID.value = Randoms.randomWifiSsid()
        wifiBSSID.value = Randoms.randomBssid()
        wifiMacAddress.value = Randoms.randomMacAddress()

        Randoms.randomCoordinates().let { (lat, lon) ->
            latitude.value = lat.toString()
            longitude.value = lon.toString()
        }

        simOperatorName.value = values.carrier.name
        simOperator.value = values.carrier.plmn
        simCountry.value = values.carrier.countryCode.lowercase()

        androidId.value = Randoms.randomAndroidId()
        imei.value = Randoms.randomIMEI()
        phoneNum.value = Randoms.randomPhoneNum()

        batteryLevel.value = Randoms.randomBatteryLevel().toString()
        timeZone.value = TimeZonePresetRepository.randomId()
    }
}

private data class RandomSelection(
    val brand: String,
    val manufacturer: String,
    val model: String,
    val device: String,
    val android: String,
    val carrier: CarrierPreset,
)

/**
 * 运营商三件套必须同源：PLMN 的 MCC 就是国家码的另一份来源（SimHook 会把 MCC/MNC 同时写进
 * getSimOperator 与 CellIdentity），三者各自随机会出现「名字是 Vodafone、国家码是 cn」这种矛盾。
 * 返回抽中的那条，供调用方取自己那一项写回输入框。
 */
fun ModuleConfigState.applyRandomCarrier(presets: List<CarrierPreset>): CarrierPreset {
    val carrier = presets.randomValidCarrier()
    simOperator.value = carrier.plmn
    simOperatorName.value = carrier.name
    simCountry.value = carrier.countryCode.lowercase()
    return carrier
}

private fun String.fingerprintSafePart(): String =
    trim().replace(Regex("[\\s/:]+"), "_").ifBlank { "device" }

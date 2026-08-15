package com.houvven.guise.ui.utils

import android.content.Context
import com.houvven.guise.device.DeviceCatalogRepository
import com.houvven.guise.module.preset.CarrierPreset
import com.houvven.guise.module.preset.CarrierPresetRepository
import com.houvven.guise.module.preset.PresetRepository
import com.houvven.guise.module.preset.TimeZonePresetRepository
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
        val carrier = CarrierPresetRepository.get(context).random()
        RandomSelection(
            brand = brand.buildBrand,
            manufacturer = brand.manufacturer,
            model = device.model,
            device = device.device,
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

        wifiSSID.value = Randoms.randomString(10)
        wifiBSSID.value = Randoms.randomMacAddress()
        wifiMacAddress.value = Randoms.randomMacAddress()

        Randoms.randomCoordinates().let { (lat, lon) ->
            latitude.value = lat.toString()
            longitude.value = lon.toString()
        }

        simOperatorName.value = values.carrier.name
        simOperator.value = values.carrier.plmn
        simCountry.value = values.carrier.countryCode

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

private fun String.fingerprintSafePart(): String =
    trim().replace(Regex("[\\s/:]+"), "_").ifBlank { "device" }

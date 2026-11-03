package com.houvven.guise.ui.routing.editor

import com.houvven.guise.device.DeviceBrand
import com.houvven.guise.util.android.Randoms
import com.houvven.guise.xposed.config.ModuleConfigState

internal fun applyDevicePreset(state: ModuleConfigState, value: String) {
    state.model.value = value.substringBefore(":")
    state.device.value = value.substringAfter(":", missingDelimiterValue = "")
    // MobileModels does not provide Build.PRODUCT. Keep it independent instead of
    // guessing that PRODUCT equals either MODEL or DEVICE for every manufacturer.
}

/**
 * 在该品牌的型号里随机抽一台，并顺手重算 Build ID 与 FingerPrint。
 *
 * 只换型号而不重算指纹会留下「Build.MODEL 是新机、FINGERPRINT 还是旧机」的矛盾 —— 指纹串里本就
 * 嵌着 product/device/buildId。Build.PRODUCT 数据库里没有，仍然不猜。
 * 返回 `<model>:<device>`，与型号预设的 value 同格式，交给 [applyDevicePreset] 落地。
 */
internal fun randomDevicePreset(state: ModuleConfigState, brand: DeviceBrand?): String {
    val picked = brand?.models?.randomOrNull()
        ?: return "${state.model.value}:${state.device.value}"
    val device = picked.resolvedDevice()
    val buildId = Randoms.randomBuildId(state.androidVersion.value)

    state.buildId.value = buildId
    state.fingerPrint.value = Randoms.randomFingerprint(
        brand = state.brand.value,
        product = state.product.value,
        device = device,
        androidVersion = state.androidVersion.value,
        buildId = buildId,
    )
    return "${picked.model}:$device"
}

internal fun applySelectedBrandIdentity(
    state: ModuleConfigState,
    brand: String,
    manufacturer: String,
) {
    val previousBrand = state.brand.value
    val shouldUpdateManufacturer = state.manufacturer.value.isBlank() ||
        state.manufacturer.value.equals(previousBrand, ignoreCase = true)
    state.brand.value = brand
    if (shouldUpdateManufacturer) state.manufacturer.value = manufacturer
}

internal fun normalizeConfiguredBrandIdentity(
    state: ModuleConfigState,
    brand: String,
    manufacturer: String,
) {
    val previousBrand = state.brand.value
    val manufacturerFollowedBrand = state.manufacturer.value.isNotBlank() &&
        state.manufacturer.value.equals(previousBrand, ignoreCase = true)
    state.brand.value = brand
    if (manufacturerFollowedBrand) state.manufacturer.value = manufacturer
}

internal fun resolveManufacturer(
    previousBrand: String,
    newBrand: String,
    manufacturer: String,
): String = if (
    manufacturer.isNotBlank() && manufacturer == previousBrand
) newBrand else manufacturer
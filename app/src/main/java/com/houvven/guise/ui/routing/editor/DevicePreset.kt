package com.houvven.guise.ui.routing.editor

import com.houvven.guise.xposed.config.ModuleConfigState

internal fun applyDevicePreset(state: ModuleConfigState, value: String) {
    state.model.value = value.substringBefore(":")
    state.device.value = value.substringAfter(":", missingDelimiterValue = "")
    // MobileModels does not provide Build.PRODUCT. Keep it independent instead of
    // guessing that PRODUCT equals either MODEL or DEVICE for every manufacturer.
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
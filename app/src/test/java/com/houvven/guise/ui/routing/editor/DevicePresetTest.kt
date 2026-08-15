package com.houvven.guise.ui.routing.editor

import com.houvven.guise.xposed.config.ModuleConfig
import org.junit.Assert.assertEquals
import org.junit.Test

class DevicePresetTest {
    @Test
    fun selectingBrandUsesCanonicalBrandAndManufacturer() {
        val state = ModuleConfig(brand = "oppo", manufacturer = "oppo").toModuleConfigState()
        applySelectedBrandIdentity(state, brand = "OPPO", manufacturer = "OPPO")
        assertEquals("OPPO", state.brand.value)
        assertEquals("OPPO", state.manufacturer.value)
    }

    @Test
    fun selectingBrandFillsDefaultManufacturer() {
        val state = ModuleConfig(brand = "", manufacturer = "").toModuleConfigState()
        applySelectedBrandIdentity(state, brand = "OnePlus", manufacturer = "OnePlus")
        assertEquals("OnePlus", state.brand.value)
        assertEquals("OnePlus", state.manufacturer.value)
    }

    @Test
    fun normalizingOldConfigSupportsDifferentBrandAndManufacturerCasing() {
        val state = ModuleConfig(brand = "google", manufacturer = "google").toModuleConfigState()
        normalizeConfiguredBrandIdentity(state, brand = "google", manufacturer = "Google")
        assertEquals("google", state.brand.value)
        assertEquals("Google", state.manufacturer.value)
    }

    @Test
    fun normalizingOldConfigKeepsBlankManufacturerBlank() {
        val state = ModuleConfig(brand = "oppo", manufacturer = "").toModuleConfigState()
        normalizeConfiguredBrandIdentity(state, brand = "OPPO", manufacturer = "OPPO")
        assertEquals("OPPO", state.brand.value)
        assertEquals("", state.manufacturer.value)
    }

    @Test
    fun selectingBrandPreservesExplicitManufacturerOverride() {
        val state = ModuleConfig(brand = "vivo", manufacturer = "BBK").toModuleConfigState()
        applySelectedBrandIdentity(state, brand = "OPPO", manufacturer = "OPPO")
        assertEquals("OPPO", state.brand.value)
        assertEquals("BBK", state.manufacturer.value)
    }

    @Test
    fun savingManuallyChangedBrandUpdatesManufacturerThatExactlyFollowedOldBrand() {
        assertEquals("OPPO", resolveManufacturer("vivo", "OPPO", "vivo"))
    }

    @Test
    fun savingPreservesCanonicalManufacturerWithDifferentCasing() {
        assertEquals("Google", resolveManufacturer("google", "google", "Google"))
    }

    @Test
    fun selectingDeviceDoesNotInferProductFromDeviceCode() {
        val state = ModuleConfig(
            model = "old-model",
            device = "old-device",
            product = "user-product",
        ).toModuleConfigState()
        applyDevicePreset(state, "new-model:new-device")
        assertEquals("new-model", state.model.value)
        assertEquals("new-device", state.device.value)
        assertEquals("user-product", state.product.value)
    }
}
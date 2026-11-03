package com.houvven.guise.ui.routing.editor

import com.houvven.guise.device.DeviceBrand
import com.houvven.guise.device.DeviceModel
import com.houvven.guise.xposed.config.ModuleConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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

    private val xiaomi = DeviceBrand(
        key = "xiaomi",
        displayName = "小米",
        buildBrand = "Xiaomi",
        manufacturer = "Xiaomi",
        models = listOf(
            DeviceModel(model = "23127PN0CC", name = "Xiaomi 14", device = "houji", alias = "houji", type = "mob"),
            DeviceModel(model = "M2011J18C", name = "MIX FOLD", device = "J18", alias = "cetus", type = "mob"),
        ),
    )

    @Test
    fun randomModelPullsModelDeviceAndFingerprintTogether() {
        val state = ModuleConfig(brand = "Xiaomi", androidVersion = "15").toModuleConfigState()
        val models = xiaomi.models.map { it.model }.toSet()
        val devices = xiaomi.models.map { it.resolvedDevice() }.toSet()

        repeat(50) {
            applyDevicePreset(state, randomDevicePreset(state, xiaomi))

            assertTrue("型号不在该品牌型号池里：${state.model.value}", state.model.value in models)
            assertTrue("设备代号不在该品牌型号池里：${state.device.value}", state.device.value in devices)
            // 指纹必须跟着新型号走，否则 Build.MODEL 与 Build.FINGERPRINT 自相矛盾
            assertTrue(
                "指纹没跟上型号：${state.fingerPrint.value}",
                state.fingerPrint.value.startsWith("Xiaomi/${state.device.value}/${state.device.value}:15/"),
            )
            assertTrue("指纹里没有本次 Build ID", state.fingerPrint.value.contains(state.buildId.value))
            assertTrue(state.fingerPrint.value.endsWith(":user/release-keys"))
        }
    }

    @Test
    fun randomModelKeepsConfiguredProductInsteadOfInventingOne() {
        val state = ModuleConfig(brand = "Xiaomi", androidVersion = "15", product = "custom").toModuleConfigState()
        applyDevicePreset(state, randomDevicePreset(state, xiaomi))

        assertEquals("custom", state.product.value)
        assertTrue(state.fingerPrint.value.startsWith("Xiaomi/custom/${state.device.value}:15/"))
    }

    @Test
    fun randomModelOnUnknownBrandLeavesIdentityUntouched() {
        val state = ModuleConfig(model = "A1", device = "d1", fingerPrint = "keep", buildId = "BID")
            .toModuleConfigState()

        assertEquals("A1:d1", randomDevicePreset(state, null))
        assertEquals("keep", state.fingerPrint.value)
        assertEquals("BID", state.buildId.value)
    }
}

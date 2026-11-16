package com.houvven.guise.xposed.config

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModuleConfigManagerTest {

    @Test
    fun legacyJsonUsesDefaultRandomOffsetDistance() {
        val config = ModuleConfig.fromJson("""{"randomOffset":true}""")

        assertTrue(config.randomOffset)
        assertEquals(ModuleConfig.DEFAULT_RANDOM_OFFSET_METERS, config.randomOffsetMeters, 0.0)
    }

    @Test
    fun stateChangesAreMappedWithoutReflection() {
        val config = ModuleConfig(packageName = "com.example.target")
        val manager = ModuleConfigManager.of(config)

        assertFalse(manager.hasUnsavedChanges())

        manager.state.timeZone.value = "Asia/Calcutta"
        manager.state.manufacturer.value = "Xiaomi"
        manager.state.buildId.value = "AP3A.250101.001"
        manager.state.densityDpi.value = "480"
        manager.state.longitude.value = "116.4074"
        manager.state.randomOffset.value = true
        manager.state.randomOffsetMeters.value = "250.5"
        manager.state.allowForceScreenshots.value = true

        assertTrue(manager.hasUnsavedChanges())

        manager.updateConfigFromState()

        assertEquals("Asia/Calcutta", config.timeZone)
        assertEquals("Xiaomi", config.manufacturer)
        assertEquals("AP3A.250101.001", config.buildId)
        assertEquals(480, config.densityDpi)
        assertEquals(116.4074, config.longitude, 0.0)
        assertTrue(config.randomOffset)
        assertEquals(250.5, config.randomOffsetMeters, 0.0)
        assertEquals(HooksValue.SCREENSHOTS_ENABLE, config.screenshotsFlag)
        assertFalse(manager.hasUnsavedChanges())
    }

    @Test
    fun emptyNumericStateUsesConfigurationDefaults() {
        val config = ModuleConfig(
            sdkInt = 37,
            densityDpi = 480,
            longitude = 10.0,
            latitude = 20.0,
        )
        val manager = ModuleConfigManager.of(config)

        manager.state.sdkInt.value = ""
        manager.state.densityDpi.value = ""
        manager.state.longitude.value = ""
        manager.state.latitude.value = ""
        manager.state.randomOffsetMeters.value = ""
        manager.updateConfigFromState()

        assertEquals(-1, config.sdkInt)
        assertEquals(-1, config.densityDpi)
        assertEquals(-1.0, config.longitude, 0.0)
        assertEquals(-1.0, config.latitude, 0.0)
        assertEquals(ModuleConfig.DEFAULT_RANDOM_OFFSET_METERS, config.randomOffsetMeters, 0.0)
    }
}

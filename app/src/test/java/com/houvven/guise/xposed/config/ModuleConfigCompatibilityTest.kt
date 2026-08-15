package com.houvven.guise.xposed.config

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ModuleConfigCompatibilityTest {

    @Test
    fun olderProfilesUseSafeDefaultsForNewFields() {
        val config = ModuleConfig.fromJson(
            """{"enabled":true,"brand":"google","model":"Pixel"}"""
        )

        assertEquals("google", config.brand)
        assertEquals("Pixel", config.model)
        assertEquals("", config.advertisingId)
        assertEquals("", config.gpuVendor)
        assertEquals("", config.gpuRenderer)
        assertEquals("", config.webViewUserAgent)
        assertEquals(-1, config.cameraCount)
        assertEquals(-1, config.visibleSimCount)
        assertFalse(config.hideExternalAudioDevices)
    }
}

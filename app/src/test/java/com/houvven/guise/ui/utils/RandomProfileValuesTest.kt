package com.houvven.guise.ui.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class RandomProfileValuesTest {

    @Test
    fun gpuVendorAndRendererAlwaysBelongToSameFamily() {
        val random = Random(41)
        repeat(500) {
            val gpu = selectGpuIdentity("Samsung", 35, random)
            when (gpu.vendor) {
                "Qualcomm" -> assertTrue(gpu.renderer.startsWith("Adreno"))
                "ARM" -> assertTrue(
                    gpu.renderer.startsWith("Mali-") || gpu.renderer.startsWith("Immortalis-"),
                )
                else -> error("Unexpected GPU vendor: ${gpu.vendor}")
            }
        }
    }

    @Test
    fun visibleCameraCountNeverExceedsRealCameraCount() {
        val random = Random(17)
        assertEquals(0, selectVisibleCameraCount(0, random))
        repeat(500) {
            val visible = selectVisibleCameraCount(5, random)
            assertTrue(visible in 1..5)
        }
    }

    @Test
    fun mobileNetworkKeepsAtLeastOneVisibleSimWhenModemExists() {
        val random = Random(23)
        repeat(500) {
            assertTrue(selectVisibleSimCount(2, true, random) in 1..2)
        }
        assertEquals(0, selectVisibleSimCount(0, true, random))
    }

    @Test
    fun nonMobileNetworkCanRepresentNoActiveSimWithoutExceedingCapacity() {
        val random = Random(29)
        val observed = buildSet {
            repeat(500) { add(selectVisibleSimCount(2, false, random)) }
        }
        assertTrue(0 in observed)
        assertTrue(1 in observed)
        assertTrue(2 in observed)
        assertTrue(observed.all { it in 0..2 })
    }

    @Test
    fun webViewUserAgentKeepsEngineButReplacesDeviceIdentity() {
        val base = "Mozilla/5.0 (Linux; Android 16; Old Device; wv) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 " +
            "Chrome/139.0.7258.94 Mobile Safari/537.36"
        val rewritten = rewriteWebViewUserAgent(base, "14", "SM-S918B")

        assertTrue(rewritten.contains("Android 14; SM-S918B; wv"))
        assertTrue(rewritten.contains("Chrome/139.0.7258.94"))
        assertFalse(rewritten.contains("Old Device"))
    }

    @Test
    fun externalAudioRandomizationProducesBothStates() {
        val random = Random(31)
        val observed = buildSet {
            repeat(200) { add(randomizeExternalAudioVisibility(random)) }
        }
        assertEquals(setOf(false, true), observed)
    }
}

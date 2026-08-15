package com.houvven.guise.xposed.hook

import org.junit.Assert.assertEquals
import org.junit.Test

class VulkanPrivacyBridgeTest {

    @Test
    fun rendererIsTrimmedBeforeNativeConfiguration() {
        assertEquals(
            "Adreno 740",
            VulkanPrivacyBridge.normalizeRenderer("  Adreno 740  "),
        )
    }

    @Test
    fun rendererIsBoundedForVulkanDeviceNameBuffer() {
        val oversized = "x".repeat(200)
        assertEquals(80, VulkanPrivacyBridge.normalizeRenderer(oversized).length)
    }

    @Test
    fun blankRendererStaysBlankAndDoesNotNeedNativeLoading() {
        assertEquals("", VulkanPrivacyBridge.normalizeRenderer("   "))
    }
}

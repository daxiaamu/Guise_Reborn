package com.houvven.guise.xposed.hook

import com.houvven.guise.xposed.config.ModuleConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class SystemPropertiesHookTest {

    @Test
    fun buildFieldsMapToMatchingAndroidProperties() {
        val values = systemPropertyProfile(
            ModuleConfig(
                brand = "google",
                model = "Pixel 9",
                device = "tokay",
                product = "tokay",
                androidVersion = "15",
                sdkInt = 35,
                fingerPrint = "google/tokay/tokay:15/AP3A/test:user/release-keys",
            )
        )

        assertEquals("google", values["ro.product.brand"])
        assertEquals("google", values["ro.product.manufacturer"])
        assertEquals("Pixel 9", values["ro.product.model"])
        assertEquals("35", values["ro.build.version.sdk"])
        assertEquals("15", values["ro.build.version.release"])
    }

    @Test
    fun unsetFieldsDoNotCreateFakeProperties() {
        val values = systemPropertyProfile(ModuleConfig(model = "Pixel"))

        assertEquals("Pixel", values["ro.product.model"])
        assertFalse(values.containsKey("ro.hardware"))
        assertFalse(values.containsKey("ro.build.version.sdk"))
    }
}

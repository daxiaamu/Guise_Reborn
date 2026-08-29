package com.houvven.guise.device

import java.io.File
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceCatalogTest {
    private val catalog: DeviceCatalog by lazy {
        val file = sequenceOf(
            File("src/main/assets/devices.json"),
            File("app/src/main/assets/devices.json"),
        ).first(File::isFile)
        Json.decodeFromString<DeviceCatalog>(file.readText())
    }

    @Test
    fun catalogHasExpectedCompleteShape() {
        assertEquals(1, catalog.schemaVersion)
        assertEquals("e950666a1e7a485f9853b7877791080c452ad72f", catalog.sourceRevision)
        assertEquals(26, catalog.brands.size)
        assertEquals(8_533, catalog.brands.sumOf { it.models.size })
        assertEquals(catalog.brands.size, catalog.brands.map { it.key }.distinct().size)

        catalog.brands.forEach { brand ->
            assertTrue(brand.key.isNotBlank())
            assertTrue(brand.displayName.isNotBlank())
            assertTrue(brand.buildBrand.isNotBlank())
            assertTrue(brand.manufacturer.isNotBlank())
            assertEquals(brand.models.size, brand.models.map { it.model }.distinct().size)
            brand.models.forEach { model ->
                assertTrue(model.model.isNotBlank())
                assertTrue(model.name.isNotBlank())
            }
        }
    }

    @Test
    fun canonicalBuildIdentitiesAreIndependentFromDisplayNamesAndKeys() {
        val onePlus = requireNotNull(catalog.brandByKey("oneplus"))
        assertEquals("一加", onePlus.displayName)
        assertEquals("OnePlus", onePlus.buildBrand)
        assertEquals("OnePlus", onePlus.manufacturer)

        val oppo = requireNotNull(catalog.brandByKey("oppo"))
        assertEquals("OPPO", oppo.buildBrand)
        assertEquals("OPPO", oppo.manufacturer)

        val google = requireNotNull(catalog.brandByKey("google"))
        assertEquals("google", google.buildBrand)
        assertEquals("Google", google.manufacturer)

        val findX9Ultra = requireNotNull(oppo.models.firstOrNull { it.model == "PMA110" })
        assertEquals("", findX9Ultra.device)
        assertEquals("lighthouse", findX9Ultra.alias)
    }

    @Test
    fun buildDevicePrefersCodenameAliasesAndFallsBackSafely() {
        val xiaomi = requireNotNull(catalog.brandByKey("xiaomi"))
        val xiaomi14 = requireNotNull(xiaomi.models.firstOrNull { it.model == "23127PN0CC" })
        assertEquals("N3", xiaomi14.device)
        assertEquals("houji", xiaomi14.alias)
        assertEquals("houji", xiaomi14.resolvedDevice())

        val onePlus = requireNotNull(catalog.brandByKey("oneplus"))
        val onePlus12 = requireNotNull(onePlus.models.firstOrNull { it.model == "PJD110" })
        assertEquals("", onePlus12.device)
        assertEquals("waffle", onePlus12.alias)
        assertEquals("waffle", onePlus12.resolvedDevice())

        assertEquals(
            "legacy-device",
            DeviceModel(
                model = "model",
                name = "name",
                device = "legacy-device",
                code = "code",
                type = "mob",
            ).resolvedDevice(),
        )
    }
}

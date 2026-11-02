package com.houvven.guise.module.preset

import java.io.File
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class CarrierPresetTest {

    private val carriers: List<CarrierPreset> by lazy {
        val file = sequenceOf(
            File("src/main/assets/carriers.json"),
            File("app/src/main/assets/carriers.json"),
        ).first(File::isFile)
        Json.decodeFromString<List<CarrierPreset>>(file.readText())
    }

    @Test
    fun catalogKeepsEnoughUsableEntries() {
        assertEquals(1_597, carriers.size)
        assertEquals(7, carriers.count { it.countryCode == "CN" })
        // carriers.json 本来就有脏 countryCode（"AU/CC/CX"、空串…），脏数据量钉住，
        // 上游改表时这条会立刻红，避免「随机到脏数据」变成无声的偶发问题
        assertEquals(77, carriers.count { !it.hasValidIdentity })
        assertTrue(carriers.count { it.hasValidIdentity } >= 1_500)
    }

    @Test
    fun randomCarrierNeverReturnsDirtyEntry() {
        repeat(2_000) {
            val carrier = carriers.randomValidCarrier()
            assertTrue(carrier.plmn.matches(Regex("\\d{5,6}")))
            assertTrue(carrier.countryCode.matches(Regex("[A-Z]{2}")))
            assertTrue(carrier.hasValidIdentity)
        }
    }

    @Test
    fun randomCarrierKeepsThreeFieldsFromTheSameRow() {
        repeat(500) {
            val carrier = carriers.randomValidCarrier()
            val samePlmnAndCountry = carriers.filter {
                it.plmn == carrier.plmn && it.countryCode == carrier.countryCode
            }
            assertTrue(samePlmnAndCountry.any { it.name == carrier.name })
        }
    }

    @Test
    fun dirtyOnlyCatalogFailsInsteadOfReturningGarbage() {
        val dirty = listOf(
            CarrierPreset(name = "AAPT", plmn = "50514", countryCode = "AU/CC/CX", countryName = "Australia"),
            CarrierPreset(name = "NoIso", plmn = "46000", countryCode = "", countryName = "China"),
            CarrierPreset(name = "ShortPlmn", plmn = "460", countryCode = "CN", countryName = "China"),
        )
        assertThrows(IllegalArgumentException::class.java) { dirty.randomValidCarrier() }
    }

    @Test
    fun chinaCarriersCoverTheFourRealNetworks() {
        val china = carriers.filter { it.countryCode == "CN" }.map { it.plmn }.toSet()
        assertTrue(china.containsAll(setOf("46000", "46001", "46003", "46015")))
    }
}

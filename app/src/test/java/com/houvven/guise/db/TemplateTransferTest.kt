package com.houvven.guise.db

import com.houvven.guise.xposed.config.ModuleConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class TemplateTransferTest {

    @Test
    fun versionedBundleRoundTripsAndNormalizesText() {
        val source = template(
            name = "  Pixel profile  ",
            description = "  Stable profile  ",
        )

        val decoded = TemplateTransfer.decode(TemplateTransfer.encode(listOf(source)))

        assertEquals(1, decoded.size)
        assertEquals("Pixel profile", decoded.single().name)
        assertEquals("Stable profile", decoded.single().description)
        assertEquals("Pixel 9", ModuleConfig.fromJson(decoded.single().configuration).model)
    }

    @Test
    fun legacyArrayRemainsReadable() {
        val source = template()

        val decoded = TemplateTransfer.decode("[${source.serialization()}]")

        assertEquals(listOf(source), decoded)
    }

    @Test
    fun duplicateIdsKeepOnlyTheFirstTemplate() {
        val first = template(name = "First")
        val duplicate = template(name = "Second")

        val decoded = TemplateTransfer.decode(
            "[${first.serialization()},${duplicate.serialization()}]"
        )

        assertEquals(1, decoded.size)
        assertEquals("First", decoded.single().name)
    }

    @Test
    fun unsupportedOrMalformedBundlesAreRejected() {
        val encoded = TemplateTransfer.encode(listOf(template()))
        val unsupported = encoded.replace("\"schemaVersion\": 1", "\"schemaVersion\": 2")
        val malformedConfiguration = TemplateTransfer.encode(
            listOf(template().copy(configuration = "{"))
        )

        assertThrows(IllegalArgumentException::class.java) {
            TemplateTransfer.decode(TemplateTransfer.encode(emptyList()))
        }
        assertThrows(IllegalArgumentException::class.java) {
            TemplateTransfer.decode(unsupported)
        }
        assertThrows(Exception::class.java) {
            TemplateTransfer.decode(malformedConfiguration)
        }
    }

    private fun template(
        name: String = "Pixel profile",
        description: String? = null,
    ) = Template(
        id = "template-id",
        name = name,
        description = description,
        type = Template.Type.COMMON,
        configuration = ModuleConfig(model = "Pixel 9").toJson(),
        createTime = 1L,
        updateTime = 1L,
    )
}

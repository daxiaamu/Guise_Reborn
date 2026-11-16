package com.houvven.guise.ui.utils

import java.io.ByteArrayOutputStream
import java.util.Base64
import java.util.zip.Deflater
import kotlin.random.Random
import org.junit.Assert.assertThrows
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QrCodeCodecTest {

    @Test
    fun compressedQrPayloadRoundTripsWithoutAndroidBitmap() {
        val json = buildString {
            append("{\"schemaVersion\":1,\"templates\":[{")
            append("\"id\":\"demo\",\"configuration\":\"")
            repeat(100) { append("device-profile-") }
            append("\"}]}")
        }
        val payload = QrCodeCodec.encodePayload(json)
        assertTrue(payload.startsWith("guise1:"))

        val matrix = QrCodeCodec.encode(payload, 512)
        val pixels = IntArray(matrix.width * matrix.height)
        var offset = 0
        repeat(matrix.height) { y ->
            repeat(matrix.width) { x ->
                pixels[offset++] = if (matrix[x, y]) BLACK else WHITE
            }
        }

        val decoded = QrCodeCodec.decode(matrix.width, matrix.height, pixels)
        assertEquals(json, QrCodeCodec.decodePayload(decoded))
    }

    @Test
    fun legacyUncompressedPayloadStillDecodes() {
        assertEquals("legacy", QrCodeCodec.decodePayload("legacy"))
    }

    @Test
    fun malformedCompressedPayloadIsRejected() {
        assertThrows(Exception::class.java) {
            QrCodeCodec.decodePayload("guise1:not-valid-base64!")
        }
    }

    @Test
    fun multipartPayloadRoundTripsOutOfOrder() {
        val random = Random(18)
        val text = buildString {
            repeat(12_000) { append((33 + random.nextInt(90)).toChar()) }
        }
        val payloads = QrCodeCodec.encodePayloadParts(text)
        assertTrue(payloads.size > 1)
        payloads.forEach { QrCodeCodec.encode(it, 512) }

        val assembler = QrCodeCodec.PayloadAssembler()
        val results = payloads.reversed().map(assembler::accept)
        results.dropLast(1).forEach {
            assertTrue(it is QrCodeCodec.PayloadAssemblyResult.Progress)
        }
        val complete = results.last() as QrCodeCodec.PayloadAssemblyResult.Complete
        assertEquals(text, complete.text)
    }

    @Test
    fun assemblerAcceptsLegacyAndSingleQrPayloads() {
        val assembler = QrCodeCodec.PayloadAssembler()
        assertEquals(
            "legacy",
            (assembler.accept("legacy") as QrCodeCodec.PayloadAssemblyResult.Complete).text,
        )
        assertEquals(
            "compressed",
            (assembler.accept(QrCodeCodec.encodePayload("compressed")) as
                QrCodeCodec.PayloadAssemblyResult.Complete).text,
        )
    }

    @Test
    fun decompressionBombIsRejectedAtTheOutputLimit() {
        val oversized = ByteArray(2 * 1024 * 1024 + 1) { 'x'.code.toByte() }
        val deflater = Deflater(Deflater.BEST_SPEED)
        val compressed = try {
            deflater.setInput(oversized)
            deflater.finish()
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(8 * 1024)
            while (!deflater.finished()) {
                val count = deflater.deflate(buffer)
                output.write(buffer, 0, count)
            }
            output.toByteArray()
        } finally {
            deflater.end()
        }
        val payload = "guise1:" + Base64.getUrlEncoder().withoutPadding()
            .encodeToString(compressed)

        assertThrows(IllegalArgumentException::class.java) {
            QrCodeCodec.decodePayload(payload)
        }
    }

    private companion object {
        const val BLACK = -0x1000000
        const val WHITE = -0x1
    }
}

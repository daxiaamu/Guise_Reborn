package com.houvven.guise.ui.utils

import java.io.ByteArrayOutputStream
import java.util.Base64
import java.util.zip.Deflater
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

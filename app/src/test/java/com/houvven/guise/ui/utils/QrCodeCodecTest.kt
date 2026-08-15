package com.houvven.guise.ui.utils

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

    private companion object {
        const val BLACK = -0x1000000
        const val WHITE = -0x1
    }
}

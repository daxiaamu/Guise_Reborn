package com.houvven.guise.ui.utils

import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.BitMatrix
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.zip.Deflater
import java.util.zip.Inflater

internal object QrCodeCodec {
    fun encode(text: String, size: Int = DEFAULT_QR_SIZE): BitMatrix {
        require(text.isNotEmpty()) { "QR content is empty" }
        require(size in MIN_QR_SIZE..MAX_QR_SIZE) { "Invalid QR size" }
        return QRCodeWriter().encode(
            text,
            BarcodeFormat.QR_CODE,
            size,
            size,
            mapOf(
                EncodeHintType.CHARACTER_SET to "UTF-8",
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.L,
                EncodeHintType.MARGIN to 1,
            ),
        )
    }

    fun decode(width: Int, height: Int, pixels: IntArray): String {
        require(width > 0 && height > 0 && pixels.size == width * height) {
            "Invalid QR image buffer"
        }
        val source = RGBLuminanceSource(width, height, pixels)
        val bitmap = BinaryBitmap(HybridBinarizer(source))
        return MultiFormatReader().decode(
            bitmap,
            mapOf(
                DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
                DecodeHintType.CHARACTER_SET to "UTF-8",
                DecodeHintType.TRY_HARDER to true,
            ),
        ).text
    }

    fun encodePayload(text: String): String {
        val input = text.toByteArray(StandardCharsets.UTF_8)
        require(input.size <= MAX_DECOMPRESSED_BYTES) { "QR payload is too large" }
        val deflater = Deflater(Deflater.BEST_SPEED)
        return try {
            deflater.setInput(input)
            deflater.finish()
            val output = ByteArrayOutputStream(input.size.coerceAtMost(16_384))
            val buffer = ByteArray(BUFFER_SIZE)
            while (!deflater.finished()) {
                val count = deflater.deflate(buffer)
                if (count == 0 && deflater.needsInput()) break
                output.write(buffer, 0, count)
            }
            PAYLOAD_PREFIX + Base64.getUrlEncoder().withoutPadding()
                .encodeToString(output.toByteArray())
        } finally {
            deflater.end()
        }
    }

    fun decodePayload(payload: String): String {
        if (!payload.startsWith(PAYLOAD_PREFIX)) return payload
        val compressed = Base64.getUrlDecoder().decode(payload.removePrefix(PAYLOAD_PREFIX))
        require(compressed.size <= MAX_COMPRESSED_BYTES) { "Compressed QR payload is too large" }
        val inflater = Inflater()
        return try {
            inflater.setInput(compressed)
            val output = ByteArrayOutputStream(minOf(MAX_DECOMPRESSED_BYTES, compressed.size * 4))
            val buffer = ByteArray(BUFFER_SIZE)
            var total = 0
            while (!inflater.finished()) {
                val count = inflater.inflate(buffer)
                if (count == 0) {
                    require(!inflater.needsDictionary() && !inflater.needsInput()) {
                        "Invalid compressed QR payload"
                    }
                    continue
                }
                total += count
                require(total <= MAX_DECOMPRESSED_BYTES) { "QR payload is too large" }
                output.write(buffer, 0, count)
            }
            output.toString(StandardCharsets.UTF_8.name())
        } finally {
            inflater.end()
        }
    }

    const val DEFAULT_QR_SIZE = 768
    private const val MIN_QR_SIZE = 128
    private const val MAX_QR_SIZE = 1536
    private const val BUFFER_SIZE = 8 * 1024
    private const val MAX_COMPRESSED_BYTES = 512 * 1024
    private const val MAX_DECOMPRESSED_BYTES = 2 * 1024 * 1024
    private const val PAYLOAD_PREFIX = "guise1:"
}

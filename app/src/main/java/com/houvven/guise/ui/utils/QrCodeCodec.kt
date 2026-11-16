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
import java.util.zip.CRC32
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
        return PAYLOAD_PREFIX + encodeCompressedPayload(text)
    }

    fun encodePayloadParts(text: String): List<String> {
        val encoded = encodeCompressedPayload(text)
        val singlePayload = PAYLOAD_PREFIX + encoded
        if (singlePayload.length <= SINGLE_QR_PAYLOAD_CHARACTERS) return listOf(singlePayload)

        val compressed = Base64.getUrlDecoder().decode(encoded)
        val transferId = transferId(compressed)
        val chunks = encoded.chunked(MULTIPART_CHUNK_CHARACTERS)
        require(chunks.size <= MAX_MULTIPART_PARTS) {
            "Template data needs too many QR codes; export a JSON file instead"
        }
        return chunks.mapIndexed { index, chunk ->
            "$MULTIPART_PREFIX$transferId:${index + 1}:${chunks.size}:$chunk"
        }
    }

    private fun encodeCompressedPayload(text: String): String {
        val input = text.toByteArray(StandardCharsets.UTF_8)
        require(input.size <= MAX_DECOMPRESSED_BYTES) { "QR payload is too large" }
        val deflater = Deflater(Deflater.BEST_COMPRESSION)
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
            val compressed = output.toByteArray()
            require(compressed.size <= MAX_COMPRESSED_BYTES) { "Compressed QR payload is too large" }
            Base64.getUrlEncoder().withoutPadding().encodeToString(compressed)
        } finally {
            deflater.end()
        }
    }

    fun decodePayload(payload: String): String {
        if (!payload.startsWith(PAYLOAD_PREFIX)) return payload
        val compressed = Base64.getUrlDecoder().decode(payload.removePrefix(PAYLOAD_PREFIX))
        return inflate(compressed)
    }

    private fun inflate(compressed: ByteArray): String {
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

    class PayloadAssembler {
        private var transferId: String? = null
        private var totalParts = 0
        private val parts = mutableMapOf<Int, String>()

        fun accept(payload: String): PayloadAssemblyResult {
            if (!payload.startsWith(MULTIPART_PREFIX)) {
                reset()
                return PayloadAssemblyResult.Complete(decodePayload(payload))
            }

            val fields = payload.removePrefix(MULTIPART_PREFIX).split(':', limit = 4)
            require(fields.size == 4) { "Invalid multipart QR payload" }
            val incomingTransferId = fields[0]
            val partNumber = fields[1].toIntOrNull()
                ?: throw IllegalArgumentException("Invalid QR part number")
            val incomingTotalParts = fields[2].toIntOrNull()
                ?: throw IllegalArgumentException("Invalid QR part count")
            val chunk = fields[3]
            require(incomingTotalParts in 2..MAX_MULTIPART_PARTS) { "Invalid QR part count" }
            require(partNumber in 1..incomingTotalParts) { "Invalid QR part number" }
            require(chunk.isNotEmpty() && chunk.length <= MULTIPART_CHUNK_CHARACTERS) {
                "Invalid QR part data"
            }

            if (transferId != incomingTransferId) {
                reset()
                transferId = incomingTransferId
                totalParts = incomingTotalParts
            }
            require(totalParts == incomingTotalParts) { "Mismatched QR part count" }
            parts.putIfAbsent(partNumber, chunk)
            if (parts.size < totalParts) {
                return PayloadAssemblyResult.Progress(parts.size, totalParts)
            }

            val encoded = (1..totalParts).joinToString(separator = "") { part ->
                requireNotNull(parts[part]) { "Missing QR part $part" }
            }
            val compressed = Base64.getUrlDecoder().decode(encoded)
            require(QrCodeCodec.transferId(compressed) == transferId) {
                "QR parts failed integrity check"
            }
            val text = inflate(compressed)
            reset()
            return PayloadAssemblyResult.Complete(text)
        }

        private fun reset() {
            transferId = null
            totalParts = 0
            parts.clear()
        }
    }

    sealed interface PayloadAssemblyResult {
        data class Progress(val receivedParts: Int, val totalParts: Int) : PayloadAssemblyResult
        data class Complete(val text: String) : PayloadAssemblyResult
    }

    private fun transferId(compressed: ByteArray): String {
        val checksum = CRC32().apply { update(compressed) }.value.toString(16)
        return "$checksum-${compressed.size}"
    }

    const val DEFAULT_QR_SIZE = 768
    private const val MIN_QR_SIZE = 128
    private const val MAX_QR_SIZE = 1536
    private const val BUFFER_SIZE = 8 * 1024
    private const val MAX_COMPRESSED_BYTES = 512 * 1024
    private const val MAX_DECOMPRESSED_BYTES = 2 * 1024 * 1024
    private const val PAYLOAD_PREFIX = "guise1:"
    private const val MULTIPART_PREFIX = "guise2:"
    private const val SINGLE_QR_PAYLOAD_CHARACTERS = 2_600
    private const val MULTIPART_CHUNK_CHARACTERS = 2_200
    private const val MAX_MULTIPART_PARTS = 64
}

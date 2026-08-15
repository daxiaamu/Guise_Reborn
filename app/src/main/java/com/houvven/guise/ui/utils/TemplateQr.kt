package com.houvven.guise.ui.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.ImageDecoder
import android.net.Uri
import kotlin.math.roundToInt

fun encodeTemplateQrBitmap(content: String): Bitmap {
    val matrix = QrCodeCodec.encode(QrCodeCodec.encodePayload(content))
    val width = matrix.width
    val height = matrix.height
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
    val row = IntArray(width)
    repeat(height) { y ->
        repeat(width) { x ->
            row[x] = if (matrix[x, y]) Color.BLACK else Color.WHITE
        }
        bitmap.setPixels(row, 0, width, 0, y, width, 1)
    }
    return bitmap
}

fun decodeTemplateQrImage(context: Context, uri: Uri): String {
    val source = ImageDecoder.createSource(context.contentResolver, uri)
    val bitmap = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        val width = info.size.width
        val height = info.size.height
        val longest = maxOf(width, height)
        if (longest > MAX_QR_DECODE_DIMENSION) {
            val scale = MAX_QR_DECODE_DIMENSION.toDouble() / longest
            decoder.setTargetSize(
                (width * scale).roundToInt().coerceAtLeast(1),
                (height * scale).roundToInt().coerceAtLeast(1),
            )
        }
    }
    return try {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        QrCodeCodec.decodePayload(QrCodeCodec.decode(width, height, pixels))
    } finally {
        bitmap.recycle()
    }
}

private const val MAX_QR_DECODE_DIMENSION = 1024

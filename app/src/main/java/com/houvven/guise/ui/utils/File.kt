package com.houvven.guise.ui.utils

import android.content.ContentValues
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import com.houvven.guise.ContextAmbient

fun saveFileToDownloadDir(
    fileName: String,
    content: String,
    mimeType: String = mimeTypeFor(fileName),
) = runCatching {
    val resolver = ContextAmbient.current.contentResolver
    val uri = createDownloadUri(fileName, mimeType)
    runCatching {
        resolver.openOutputStream(uri, "w")!!.bufferedWriter().use { it.write(content) }
    }.onFailure {
        resolver.delete(uri, null, null)
    }.getOrThrow()
    uri
}

fun saveBitmapToDownloadDir(
    fileName: String,
    bitmap: Bitmap,
) = runCatching {
    val resolver = ContextAmbient.current.contentResolver
    val uri = createDownloadUri(fileName, "image/png")
    runCatching {
        resolver.openOutputStream(uri, "w")!!.use { output ->
            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) {
                "Unable to encode PNG"
            }
        }
    }.onFailure {
        resolver.delete(uri, null, null)
    }.getOrThrow()
    uri
}

private fun createDownloadUri(fileName: String, mimeType: String): Uri {
    val resolver = ContextAmbient.current.contentResolver
    val values = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
        put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
        put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/Guise")
    }
    return requireNotNull(resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)) {
        "Unable to create Downloads/Guise/$fileName"
    }
}

private fun mimeTypeFor(fileName: String): String = when (
    fileName.substringAfterLast('.', "").lowercase()
) {
    "json" -> "application/json"
    "png" -> "image/png"
    "txt", "log" -> "text/plain"
    else -> "application/octet-stream"
}

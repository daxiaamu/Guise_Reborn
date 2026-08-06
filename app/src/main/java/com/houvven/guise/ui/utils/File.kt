package com.houvven.guise.ui.utils

import android.content.ContentValues
import android.provider.MediaStore
import com.houvven.guise.ContextAmbient

fun saveFileToDownloadDir(
    fileName: String,
    content: String,
    mimeType: String = mimeTypeFor(fileName),
) = runCatching {
    val resolver = ContextAmbient.current.contentResolver
    val values = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
        put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
        put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/Guise")
    }
    val uri = requireNotNull(resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)) {
        "Unable to create Downloads/Guise/$fileName"
    }
    runCatching {
        resolver.openOutputStream(uri, "w")!!.bufferedWriter().use { it.write(content) }
    }.onFailure {
        resolver.delete(uri, null, null)
    }.getOrThrow()
    uri
}

private fun mimeTypeFor(fileName: String): String = when (
    fileName.substringAfterLast('.', "").lowercase()
) {
    "json" -> "application/json"
    "txt", "log" -> "text/plain"
    else -> "application/octet-stream"
}

package com.houvven.guise.ui.routing.launcher

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import com.houvven.guise.BuildConfig
import com.houvven.guise.R
import com.houvven.guise.db.Template
import com.houvven.guise.db.TemplateTransfer
import com.houvven.guise.ui.GlobalSnackbarHost
import com.houvven.guise.ui.routing.LauncherState
import com.houvven.guise.ui.utils.encodeTemplateQrBitmaps
import com.houvven.guise.ui.utils.saveBitmapToDownloadDir
import com.houvven.guise.ui.utils.saveFileToDownloadDir
import java.io.Reader
import java.net.HttpURLConnection
import java.net.URI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal enum class TemplateTransferMode { IMPORT, EXPORT }

@Composable
internal fun TemplateTransferDialog(
    mode: TemplateTransferMode,
    onDismiss: () -> Unit,
    onScanQr: () -> Unit,
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    var showUrlDialog by remember { mutableStateOf(false) }
    var importUrl by remember { mutableStateOf("") }
    var qrPreview by remember { mutableStateOf<List<Bitmap>?>(null) }
    var qrPage by remember { mutableStateOf(0) }

    DisposableEffect(qrPreview) {
        val currentPreview = qrPreview
        onDispose { currentPreview?.forEach(Bitmap::recycle) }
    }

    suspend fun accept(result: Result<List<Template>>) {
        val imported = result.getOrNull()
        if (imported != null) {
            LauncherState.addTemplates(imported)
            GlobalSnackbarHost.showByDismissPrevious(resources.getString(R.string.import_success))
        } else {
            val error = result.exceptionOrNull()
            GlobalSnackbarHost.showOnErrorByDismissPrevious(
                resources.getString(R.string.import_failed, error?.message.orEmpty())
            )
        }
    }

    val fileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            accept(withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use {
                        TemplateTransfer.decode(it.readLimitedText(MAX_TEMPLATE_IMPORT_CHARS))
                    } ?: error("Unable to open selected file")
                }
            })
            onDismiss()
        }
    }

    fun exportJson() = scope.launch {
        runCatching {
            val encoded = withContext(Dispatchers.Default) {
                TemplateTransfer.encode(LauncherState.templates.value)
            }
            withContext(Dispatchers.IO) {
                saveFileToDownloadDir(
                    "Guise-Templates-${System.currentTimeMillis()}.json",
                    encoded,
                ).getOrThrow()
            }
        }.onSuccess {
            GlobalSnackbarHost.showByDismissPrevious(resources.getString(R.string.export_success, it))
        }.onFailure {
            GlobalSnackbarHost.showOnErrorByDismissPrevious(
                resources.getString(R.string.export_failed, it.message.orEmpty())
            )
        }
        onDismiss()
    }

    fun createQr() = scope.launch {
        runCatching {
            withContext(Dispatchers.Default) {
                encodeTemplateQrBitmaps(TemplateTransfer.encode(LauncherState.templates.value))
            }
        }.onSuccess { bitmaps ->
            qrPreview?.forEach(Bitmap::recycle)
            qrPage = 0
            qrPreview = bitmaps
        }.onFailure {
            GlobalSnackbarHost.showOnErrorByDismissPrevious(
                resources.getString(R.string.export_failed, it.message.orEmpty())
            )
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (mode == TemplateTransferMode.IMPORT) R.string.import_data else R.string.export_data)) },
        text = {
            Column {
                if (mode == TemplateTransferMode.IMPORT) {
                    TransferChoice(stringResource(R.string.import_json_file)) {
                        fileLauncher.launch("application/json")
                    }
                    TransferChoice(stringResource(R.string.scan_qr_code)) {
                        onDismiss(); onScanQr()
                    }
                    TransferChoice(stringResource(R.string.import_from_url)) {
                        showUrlDialog = true
                    }
                } else {
                    TransferChoice(stringResource(R.string.export_json_file)) {
                        exportJson()
                    }
                    TransferChoice(stringResource(R.string.show_qr_code)) {
                        createQr()
                    }

                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )

    if (showUrlDialog) {
        AlertDialog(
            onDismissRequest = { showUrlDialog = false },
            title = { Text(stringResource(R.string.import_from_url)) },
            text = {
                OutlinedTextField(
                    value = importUrl,
                    onValueChange = { importUrl = it },
                    label = { Text(stringResource(R.string.template_url)) },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(enabled = importUrl.isNotBlank(), onClick = {
                    val source = importUrl.trim()
                    showUrlDialog = false
                    scope.launch {
                        accept(withContext(Dispatchers.IO) {
                            runCatching { TemplateTransfer.decode(fetchTemplateJson(source)) }
                        })
                        onDismiss()
                    }
                }) { Text(stringResource(R.string.import_data)) }
            },
            dismissButton = {
                TextButton(onClick = { showUrlDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    qrPreview?.let { bitmaps ->
        val bitmap = bitmaps[qrPage]
        fun closePreview() {
            bitmaps.forEach(Bitmap::recycle)
            qrPreview = null
            onDismiss()
        }
        AlertDialog(
            onDismissRequest = ::closePreview,
            title = {
                Text(
                    if (bitmaps.size == 1) stringResource(R.string.template_qr_code)
                    else stringResource(R.string.template_qr_code_part, qrPage + 1, bitmaps.size)
                )
            },
            text = {
                Column {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = stringResource(R.string.template_qr_code),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (bitmaps.size > 1) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            TextButton(
                                enabled = qrPage > 0,
                                onClick = { qrPage-- },
                            ) { Text(stringResource(R.string.previous_qr_code)) }
                            TextButton(
                                enabled = qrPage < bitmaps.lastIndex,
                                onClick = { qrPage++ },
                            ) { Text(stringResource(R.string.next_qr_code)) }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = ::closePreview) {
                    Text(stringResource(R.string.close))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    scope.launch {
                        runCatching {
                            withContext(Dispatchers.IO) {
                                val timestamp = System.currentTimeMillis()
                                bitmaps.mapIndexed { index, part ->
                                    val suffix = if (bitmaps.size == 1) "" else
                                        "-part-${index + 1}-of-${bitmaps.size}"
                                    saveBitmapToDownloadDir(
                                        "Guise-Templates-$timestamp$suffix.png",
                                        part,
                                    ).getOrThrow()
                                }
                            }
                        }.onSuccess { paths ->
                            GlobalSnackbarHost.showByDismissPrevious(
                                resources.getString(R.string.export_success, paths.last())
                            )
                            closePreview()
                        }.onFailure {
                            GlobalSnackbarHost.showOnErrorByDismissPrevious(
                                resources.getString(R.string.export_failed, it.message.orEmpty())
                            )
                        }
                    }
                }) { Text(stringResource(R.string.save)) }
            },
        )
    }
}

@Composable
private fun TransferChoice(text: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(text) },
        modifier = Modifier.clickable(onClick = onClick),
    )
}

private fun fetchTemplateJson(source: String): String {
    val uri = URI(source)
    require(uri.scheme.equals("https", true)) {
        "Only HTTPS URLs are supported"
    }
    val connection = uri.toURL().openConnection() as HttpURLConnection
    return try {
        connection.connectTimeout = 10_000
        connection.readTimeout = 15_000
        connection.instanceFollowRedirects = true
        connection.setRequestProperty("Accept", "application/json, text/plain;q=0.9")
        connection.setRequestProperty("User-Agent", "Guise/${BuildConfig.VERSION_NAME}")
        require(connection.responseCode in 200..299) { "HTTP ${connection.responseCode}" }
        require(connection.contentLengthLong < 0 || connection.contentLengthLong <= MAX_TEMPLATE_IMPORT_BYTES) {
            "Template file is too large"
        }
        connection.inputStream.bufferedReader().use {
            it.readLimitedText(MAX_TEMPLATE_IMPORT_CHARS)
        }
    } finally {
        connection.disconnect()
    }
}

private fun Reader.readLimitedText(maxChars: Int): String {
    val output = StringBuilder(minOf(DEFAULT_BUFFER_SIZE, maxChars))
    val buffer = CharArray(DEFAULT_BUFFER_SIZE)
    var total = 0
    while (true) {
        val count = read(buffer)
        if (count < 0) return output.toString()
        total += count
        require(total <= maxChars) { "Template file is too large" }
        output.append(buffer, 0, count)
    }
}

private const val MAX_TEMPLATE_IMPORT_CHARS = 2_000_000
private const val MAX_TEMPLATE_IMPORT_BYTES = 2_000_000L

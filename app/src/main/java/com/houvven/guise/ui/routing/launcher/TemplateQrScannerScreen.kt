package com.houvven.guise.ui.routing.launcher

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.houvven.guise.R
import com.houvven.guise.db.TemplateTransfer
import com.houvven.guise.ui.GlobalSnackbarHost
import com.houvven.guise.ui.routing.LauncherState
import com.houvven.guise.ui.routing.LocalNavController
import com.houvven.guise.ui.utils.QrCodeCodec
import com.houvven.guise.ui.utils.decodeTemplateQrImage
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun TemplateQrScannerScreen() {
    val context = LocalContext.current
    val resources = LocalResources.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val navController = LocalNavController.current
    val scope = rememberCoroutineScope()
    var cameraAllowed by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    val handled = remember { AtomicBoolean(false) }
    val lastScanAttempt = remember { AtomicLong(0L) }

    suspend fun accept(content: String) {
        if (!handled.compareAndSet(false, true)) return
        runCatching { TemplateTransfer.decode(content) }
            .onSuccess {
                LauncherState.addTemplates(it)
                GlobalSnackbarHost.showByDismissPrevious(resources.getString(R.string.import_success))
                navController.popBackStack()
            }
            .onFailure {
                handled.set(false)
                GlobalSnackbarHost.showOnErrorByDismissPrevious(
                    resources.getString(R.string.import_failed, it.message.orEmpty())
                )
            }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { cameraAllowed = it }
    val imageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) scope.launch {
            runCatching {
                withContext(Dispatchers.IO) { decodeTemplateQrImage(context, uri) }
            }.onSuccess { accept(it) }
                .onFailure {
                    GlobalSnackbarHost.showOnErrorByDismissPrevious(
                        resources.getString(R.string.import_failed, it.message.orEmpty())
                    )
                }
        }
    }

    LaunchedEffect(Unit) {
        if (!cameraAllowed) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    DisposableEffect(cameraAllowed, previewView, lifecycleOwner) {
        val view = previewView
        if (!cameraAllowed || view == null) return@DisposableEffect onDispose { }
        val executor = Executors.newSingleThreadExecutor()
        val providerFuture = ProcessCameraProvider.getInstance(context)
        var disposed = false
        val listener = Runnable {
            if (disposed) return@Runnable
            val provider = providerFuture.get()
            val preview = Preview.Builder().build().also {
                it.surfaceProvider = view.surfaceProvider
            }
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()
            analysis.setAnalyzer(executor) { image ->
                try {
                    val now = System.currentTimeMillis()
                    if (!handled.get() && now - lastScanAttempt.get() >= SCAN_INTERVAL_MS) {
                        lastScanAttempt.set(now)
                        runCatching { decodeRgbaQr(image) }.getOrNull()?.let { value ->
                            scope.launch { accept(value) }
                        }
                    }
                } finally {
                    image.close()
                }
            }
            runCatching {
                provider.unbindAll()
                provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
            }
        }
        providerFuture.addListener(listener, ContextCompat.getMainExecutor(context))
        onDispose {
            disposed = true
            if (providerFuture.isDone) {
                runCatching { providerFuture.get().unbindAll() }
            }
            executor.shutdownNow()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.scan_qr_code)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding).background(ComposeColor.Black),
            contentAlignment = Alignment.Center,
        ) {
            if (cameraAllowed) {
                AndroidView(
                    factory = { PreviewView(it).also { view -> previewView = view } },
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Text(
                    text = stringResource(R.string.camera_permission_required),
                    color = ComposeColor.White,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Button(onClick = { imageLauncher.launch("image/*") }) {
                    Icon(Icons.Default.Image, contentDescription = null)
                    Text(stringResource(R.string.choose_qr_image), modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }
}

private fun decodeRgbaQr(image: ImageProxy): String {
    val plane = image.planes.first()
    val buffer = plane.buffer
    val width = image.width
    val height = image.height
    val pixels = IntArray(width * height)
    val rowStride = plane.rowStride
    val pixelStride = plane.pixelStride
    for (y in 0 until height) {
        val rowStart = y * rowStride
        for (x in 0 until width) {
            val offset = rowStart + x * pixelStride
            pixels[y * width + x] = Color.argb(
                buffer.get(offset + 3).toInt() and 0xff,
                buffer.get(offset).toInt() and 0xff,
                buffer.get(offset + 1).toInt() and 0xff,
                buffer.get(offset + 2).toInt() and 0xff,
            )
        }
    }
    return QrCodeCodec.decode(width, height, pixels)
}

private const val SCAN_INTERVAL_MS = 300L

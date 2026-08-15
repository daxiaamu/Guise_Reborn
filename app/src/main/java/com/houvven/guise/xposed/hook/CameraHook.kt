package com.houvven.guise.xposed.hook

import android.hardware.Camera
import android.hardware.camera2.CameraManager
import com.houvven.guise.xposed.LoadPackageHandler
import com.houvven.ktx_xposed.hook.afterHookedMethod

@Suppress("DEPRECATION")
class CameraHook : LoadPackageHandler {
    override fun onHook() {
        val limit = config.cameraCount.coerceAtLeast(0)
        Camera::class.java.afterHookedMethod("getNumberOfCameras") { param ->
            val actual = param.result as? Int ?: return@afterHookedMethod
            param.result = minOf(actual, limit)
        }
        CameraManager::class.java.afterHookedMethod("getCameraIdList") { param ->
            val ids = param.result as? Array<*> ?: return@afterHookedMethod
            param.result = limitVisibleCameraIds(ids.filterIsInstance<String>(), limit)
        }
    }
}

internal fun limitVisibleCameraIds(ids: List<String>, limit: Int): Array<String> =
    ids.take(limit.coerceAtLeast(0)).toTypedArray()

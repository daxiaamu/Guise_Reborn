package com.houvven.guise.xposed.hook

import android.opengl.GLES10
import android.opengl.GLES20
import com.houvven.guise.xposed.LoadPackageHandler
import com.houvven.ktx_xposed.hook.beforeHookedMethod
import com.houvven.ktx_xposed.logger.XposedLogger

class GpuHook : LoadPackageHandler {
    override fun onHook() {
        hookGlIdentity(GLES10::class.java)
        hookGlIdentity(GLES20::class.java)
        config.gpuRenderer.takeIf(String::isNotBlank)?.let { renderer ->
            VulkanPrivacyBridge.configure(renderer).onFailure { XposedLogger.e(it) }
        }
    }

    private fun hookGlIdentity(clazz: Class<*>) {
        clazz.beforeHookedMethod("glGetString", Int::class.javaPrimitiveType!!) { param ->
            when (param.args.firstOrNull() as? Int) {
                GLES20.GL_VENDOR -> config.gpuVendor.takeIf(String::isNotBlank)?.let {
                    param.result = it
                }
                GLES20.GL_RENDERER -> config.gpuRenderer.takeIf(String::isNotBlank)?.let {
                    param.result = it
                }
            }
        }
    }
}

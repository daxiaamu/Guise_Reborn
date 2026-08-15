package com.houvven.guise.xposed.hook

import com.houvven.guise.xposed.LoadPackageHandler
import com.houvven.guise.xposed.config.ModuleConfig
import com.houvven.ktx_xposed.hook.beforeHookAllMethods
import com.houvven.ktx_xposed.hook.findClassIfExists

class SystemPropertiesHook : LoadPackageHandler {
    override fun onHook() {
        val values = systemPropertyProfile(config)
        if (values.isEmpty()) return
        val systemProperties = findClassIfExists("android.os.SystemProperties") ?: return

        systemProperties.beforeHookAllMethods("get") { param ->
            val key = param.args.firstOrNull() as? String ?: return@beforeHookAllMethods
            values[key]?.let { param.result = it }
        }
        systemProperties.beforeHookAllMethods("getInt") { param ->
            val key = param.args.firstOrNull() as? String ?: return@beforeHookAllMethods
            values[key]?.toIntOrNull()?.let { param.result = it }
        }
        systemProperties.beforeHookAllMethods("getLong") { param ->
            val key = param.args.firstOrNull() as? String ?: return@beforeHookAllMethods
            values[key]?.toLongOrNull()?.let { param.result = it }
        }
    }
}

internal fun systemPropertyProfile(config: ModuleConfig): Map<String, String> = buildMap {
    fun putValue(key: String, value: String) {
        value.takeIf(String::isNotBlank)?.let { put(key, it) }
    }

    putValue("ro.product.brand", config.brand)
    putValue("ro.product.manufacturer", config.manufacturer.ifBlank { config.brand })
    putValue("ro.product.model", config.model)
    putValue("ro.product.name", config.product)
    putValue("ro.product.device", config.device)
    putValue("ro.product.board", config.board)
    putValue("ro.hardware", config.hardware)
    putValue("ro.build.id", config.buildId)
    putValue("ro.build.version.release", config.androidVersion)
    config.sdkInt.takeIf { it >= 0 }?.let { put("ro.build.version.sdk", it.toString()) }
    putValue("ro.build.fingerprint", config.fingerPrint)
}

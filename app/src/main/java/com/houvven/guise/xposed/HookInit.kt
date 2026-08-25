package com.houvven.guise.xposed

import android.app.Application
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.util.Log
import com.houvven.guise.BuildConfig
import com.houvven.guise.xposed.hook.BatteryHook
import com.houvven.guise.xposed.hook.BuildConfigHook
import com.houvven.guise.xposed.hook.DisplayDensityHook
import com.houvven.guise.xposed.hook.LocalHook
import com.houvven.guise.xposed.hook.OsBuildHook
import com.houvven.guise.xposed.hook.ScreenshotsHook
import com.houvven.guise.xposed.hook.TimeZoneHook
import com.houvven.guise.xposed.hook.UniquelyIdHook
import com.houvven.guise.xposed.hook.location.CellLocationHook
import com.houvven.guise.xposed.hook.location.LocationHook
import com.houvven.guise.xposed.hook.netowork.NetworkHook
import com.houvven.guise.xposed.other.ApplicationListPass
import com.houvven.guise.xposed.other.BlankPass
import com.houvven.ktx_xposed.LoadPackageHookAdapter
import com.houvven.ktx_xposed.hook.LoadPackageContext
import com.houvven.ktx_xposed.hook.ModernXposedRuntime
import com.houvven.ktx_xposed.logger.XposedLogger
import com.houvven.ktx_xposed.utils.runXposedCatching
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.HotReloadingParam
import io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam
import kotlin.system.exitProcess

@Suppress("unused")
class HookInit : XposedModule() {

    private var processName: String = ""

    override fun onModuleLoaded(param: ModuleLoadedParam) {
        processName = param.processName
    }

    override fun onHotReloading(param: HotReloadingParam): Boolean {
        if (scheduleProcessExit(param.extras)) return false
        return super.onHotReloading(param)
    }

    override fun onPackageReady(param: PackageReadyParam) {
        if (!param.isFirstPackage || param.packageName == BuildConfig.APPLICATION_ID) return

        ModernXposedRuntime.initialize(
            this,
            LoadPackageContext(param.packageName, processName, param.classLoader),
        )
        ModernXposedPreferences.current = getRemotePreferences(PackageConfig.PREF_FILE_NAME)
        PackageConfig.doRefresh(param.packageName)
        if (!PackageConfig.current.isEnable) return

        XposedLogger.initialize(::currentApplication)
        XposedLogger.d("Package ready")

        val hooks = PackageConfig.current.activeHookFeatures().map(::createHook)
        hooks.forEach { (category, hook) ->
            val setupCompleted = runXposedCatching(category) {
                XposedLogger.withCategory(category) { hook.onHook() }
                true
            } == true
            if (setupCompleted) {
                XposedLogger.d("Hook setup completed", category)
            }
        }
        XposedLogger.finishStartup()
        if (XposedLogger.needsDeliveryContext()) attachLogContextWhenReady()
    }

    private fun createHook(feature: HookFeature): Pair<String, LoadPackageHookAdapter> =
        when (feature) {
            HookFeature.BATTERY -> "Battery" to BatteryHook()
            HookFeature.LOCALE -> "Locale" to LocalHook()
            HookFeature.TIME_ZONE -> "TimeZone" to TimeZoneHook()
            HookFeature.LOCATION -> "Location" to LocationHook()
            HookFeature.CELL_LOCATION -> "CellLocation" to CellLocationHook()
            HookFeature.NETWORK -> "Network" to NetworkHook()
            HookFeature.OS_BUILD -> "OSBuild" to OsBuildHook()
            HookFeature.DISPLAY_DENSITY -> "DisplayDensity" to DisplayDensityHook()
            HookFeature.SCREENSHOTS -> "Screenshots" to ScreenshotsHook()
            HookFeature.UNIQUE_ID -> "UniqueId" to UniquelyIdHook()
            HookFeature.BLANK_PASS -> "BlankPass" to BlankPass()
            HookFeature.APPLICATION_LIST -> "ApplicationList" to ApplicationListPass()
            HookFeature.APP_VERSION -> "AppVersion" to BuildConfigHook()
        }

    /** Obtains an application context without modifying Application or Activity lifecycle methods. */
    private fun attachLogContextWhenReady(attempt: Int = 0) {
        if (XposedLogger.tryAttachContext()) return
        if (!XposedLogger.needsDeliveryContext() || attempt >= LOG_CONTEXT_MAX_ATTEMPTS) return
        Handler(Looper.getMainLooper()).postDelayed(
            { attachLogContextWhenReady(attempt + 1) },
            LOG_CONTEXT_RETRY_DELAY_MS,
        )
    }

    private fun currentApplication(): Application? = runCatching {
        val method = Class.forName("android.app.ActivityThread")
            .getDeclaredMethod("currentApplication")
        getInvoker(method).invoke(null) as? Application
    }.getOrNull()

    private fun scheduleProcessExit(extras: Bundle?): Boolean {
        if (!ProcessControl.isExitRequest(extras)) return false
        log(Log.INFO, TAG, "Process exit requested for $processName")
        Handler(Looper.getMainLooper()).postDelayed({
            Process.killProcess(Process.myPid())
            exitProcess(0)
        }, PROCESS_EXIT_DELAY_MS)
        return true
    }

    companion object {
        private const val TAG = "Guise"
        private const val PROCESS_EXIT_DELAY_MS = 150L
        private const val LOG_CONTEXT_RETRY_DELAY_MS = 50L
        private const val LOG_CONTEXT_MAX_ATTEMPTS = 20
    }
}

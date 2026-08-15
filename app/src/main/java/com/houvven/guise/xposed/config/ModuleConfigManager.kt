package com.houvven.guise.xposed.config

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.core.content.edit
import com.houvven.guise.ContextAmbient
import com.houvven.guise.R
import com.houvven.guise.ui.GlobalSnackbarHost
import com.houvven.guise.ui.routing.LauncherState
import com.houvven.guise.xposed.PackageConfig
import com.houvven.guise.xposed.ProcessControl
import io.github.libxposed.service.HotReloadResult
import io.github.libxposed.service.HookedTarget
import io.github.libxposed.service.XposedService
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class ModuleConfigManager
private constructor(
    val config: ModuleConfig,
    val state: ModuleConfigState,
) {

    private val safePrefs
        get() = PackageConfig.safePrefs

    private val context
        get() = ContextAmbient.current

    fun clear() {
        state.clear()
    }

    /** Saves spoofing parameters without changing the LSPosed scope. */
    fun save() {
        this.updateConfigFromState()
        persist()
    }

    fun hasUnsavedChanges(): Boolean =
        !config.hasSameParameters(configFromState())

    fun hasNoConfiguredParameters(): Boolean =
        config.hasSameParameters(ModuleConfig())

    /** Returns null when the Xposed service cannot reliably answer. */
    fun hasRunningHookedTarget(): Boolean? {
        val service = ContextAmbient.xposedService ?: return null
        return runCatching {
            service.getRunningTargets().any(::isTargetProcess)
        }.getOrNull()
    }

    /** The single entry point for changing both selection state and LSPosed scope. */
    fun setEnabled(enabled: Boolean, notifyOnScopeError: Boolean = true) {
        config.enabled = enabled
        persist()
        syncLsposedScope(enabled, notifyOnScopeError)
    }

    private fun persist() {
        safePrefs.edit(commit = true) { putString(config.packageName, config.toJson()) }
        LauncherState.setAppEnabled(config.packageName, config.enabled)
        PackageConfig.notifyConfigurationsChanged()
    }

    private fun syncLsposedScope(enable: Boolean, notifyOnError: Boolean) {
        val service = ContextAmbient.xposedService ?: run {
            if (notifyOnError) {
                reportScopeError(context.getString(R.string.xposed_service_not_connected))
            }
            return
        }
        if (!enable) {
            runCatching { service.removeScope(listOf(config.packageName)) }
                .onFailure {
                    if (notifyOnError) reportScopeError(it.message ?: it.toString())
                }
            return
        }
        service.requestScope(
            listOf(config.packageName),
            object : XposedService.OnScopeEventListener {
                override fun onScopeRequestApproved(approved: List<String>) = Unit
                override fun onScopeRequestFailed(message: String) {
                    if (notifyOnError) reportScopeError(message)
                }
            },
        )
    }

    private fun reportScopeError(message: String) {
        GlobalSnackbarHost.showOnErrorByDismissPrevious(
            context.getString(R.string.xposed_scope_sync_failed, message)
        )
    }

    suspend fun stopApp(): Result<Unit> {
        this.save()
        return stopWithFallback()
    }

    suspend fun restartApp(): Result<Unit> {
        this.save()
        return runCatching {
            stopWithFallback().getOrThrow()
            val intent = context.packageManager.getLaunchIntentForPackage(config.packageName)
                ?: error(context.getString(R.string.app_not_launchable))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            context.startActivity(intent)
        }
    }

    suspend fun stopIfHooked(): Result<Unit> =
        requestProcessExit().map { }

    private suspend fun stopWithFallback(): Result<Unit> {
        val rootResult = forceStopWithRoot()
        if (rootResult.isSuccess) return rootResult

        val xposedResult = requestProcessExit()
        if (xposedResult.getOrNull()?.let { it > 0 } == true) return Result.success(Unit)

        openApplicationDetails()
        return Result.failure(
            IllegalStateException(context.getString(R.string.manual_force_stop_required))
        )
    }

    private suspend fun requestProcessExit(): Result<Int> = runCatching {
        val service = ContextAmbient.xposedService ?: return@runCatching 0
        val targets = service.getRunningTargets().filter(::isTargetProcess)
        if (targets.isEmpty()) return@runCatching 0
        val targetPids = targets.mapTo(mutableSetOf()) { it.pid }
        val extras = Bundle().apply {
            putString(ProcessControl.EXTRA_COMMAND, ProcessControl.COMMAND_EXIT)
        }
        targets.forEach { target ->
            service.hotReloadModule(
                target,
                Bundle(extras),
                object : XposedService.HotReloadCallback {
                    override fun onHotReloadResult(
                        target: HookedTarget,
                        result: HotReloadResult,
                    ) = Unit
                },
            )
        }
        delay(PROCESS_EXIT_WAIT_MS)
        val remainingPids = service.getRunningTargets()
            .asSequence()
            .filter(::isTargetProcess)
            .map { it.pid }
            .filterTo(mutableSetOf()) { it in targetPids }
        if (remainingPids.isNotEmpty()) {
            error(context.getString(R.string.xposed_target_process_exit_failed))
        }
        targets.size
    }

    private suspend fun forceStopWithRoot(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            check(PACKAGE_NAME_PATTERN.matches(config.packageName)) {
                context.getString(R.string.invalid_package_name)
            }
            val command =
                "am force-stop --user current ${config.packageName}"
            val process = ProcessBuilder("su", "-c", command)
                .redirectErrorStream(true)
                .start()
            if (!process.waitFor(ROOT_COMMAND_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                process.destroy()
                error(context.getString(R.string.root_force_stop_timeout))
            }
            val output = process.inputStream.bufferedReader().use { it.readText().trim() }
            check(process.exitValue() == 0) {
                output.ifBlank { context.getString(R.string.root_force_stop_failed) }
            }
        }
    }

    private fun isTargetProcess(target: HookedTarget): Boolean =
        target.processName == config.packageName ||
            target.processName.startsWith("${config.packageName}:")

    private fun openApplicationDetails() {
        context.startActivity(
            Intent(
                android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", config.packageName, null),
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    fun updateConfigFromState() {
        updateConfigFromState(config)
    }

    private fun configFromState(): ModuleConfig =
        config.copy().also(::updateConfigFromState)

    private fun updateConfigFromState(target: ModuleConfig) = with(target) {
        val previousBrand = brand
        brand = state.brand.value
        manufacturer = com.houvven.guise.ui.routing.editor.resolveManufacturer(
            previousBrand = previousBrand,
            newBrand = brand,
            manufacturer = state.manufacturer.value,
        )
        model = state.model.value
        product = state.product.value
        device = state.device.value
        board = state.board.value
        hardware = state.hardware.value
        buildId = state.buildId.value
        androidVersion = state.androidVersion.value
        sdkInt = state.sdkInt.value.toIntOrNull() ?: -1
        densityDpi = state.densityDpi.value.toIntOrNull() ?: -1
        networkType = state.networkType.value.toIntOrNull() ?: HooksValue.NET_UNHOOK
        fingerPrint = state.fingerPrint.value
        wifiSSID = state.wifiSSID.value
        wifiBSSID = state.wifiBSSID.value
        wifiMacAddress = state.wifiMacAddress.value
        simOperator = state.simOperator.value
        simOperatorName = state.simOperatorName.value
        simCountry = state.simCountry.value
        imei = state.imei.value
        phoneNum = state.phoneNum.value
        androidId = state.androidId.value
        lac = state.lac.value.toIntOrNull() ?: -1
        cid = state.cid.value.toIntOrNull() ?: -1
        language = state.language.value
        timeZone = state.timeZone.value
        longitude = state.longitude.value.toDoubleOrNull() ?: -1.0
        latitude = state.latitude.value.toDoubleOrNull() ?: -1.0
        randomOffset = state.randomOffset.value
        makeWifiLocationFail = state.makeWifiLocationFail.value
        makeCellLocationFail = state.makeCellLocationFail.value
        versionCode = state.versionCode.value.toIntOrNull() ?: -1
        versionName = state.versionName.value
        batteryLevel = state.batteryLevel.value.toIntOrNull() ?: -1
        screenshotsFlag = if (state.allowForceScreenshots.value) {
            HooksValue.SCREENSHOTS_ENABLE
        } else {
            HooksValue.SCREENSHOTS_UNHOOK
        }
        passContacts = state.passContacts.value
        passPhoto = state.passPhoto.value
        passVideo = state.passVideo.value
        passAudio = state.passAudio.value
        passApplications = state.passApplications.value
    }

    companion object {

        private const val PROCESS_EXIT_WAIT_MS = 800L
        private const val ROOT_COMMAND_TIMEOUT_SECONDS = 15L
        private val PACKAGE_NAME_PATTERN = Regex("[A-Za-z0-9._]+")

        fun of(config: ModuleConfig, state: ModuleConfigState) = ModuleConfigManager(config, state)

        fun of(config: ModuleConfig): ModuleConfigManager {
            val state = ModuleConfigState.of(config)
            return ModuleConfigManager(config, state)
        }

        fun empty() = of(ModuleConfig())

        fun reconcileScope(service: XposedService) {
            val desiredScope = ModuleConfig.getAllSaved()
                .asSequence()
                .filter { it.enabled }
                .mapTo(mutableSetOf()) { it.packageName }
            val currentScope = runCatching { service.getScope().toSet() }.getOrNull() ?: return
            val removedPackages = currentScope - desiredScope
            val addedPackages = desiredScope - currentScope
            if (removedPackages.isNotEmpty()) {
                runCatching { service.removeScope(removedPackages.toList()) }
            }
            if (addedPackages.isNotEmpty()) {
                service.requestScope(
                    addedPackages.toList(),
                    object : XposedService.OnScopeEventListener {
                        override fun onScopeRequestApproved(approved: List<String>) = Unit
                        override fun onScopeRequestFailed(message: String) = Unit
                    },
                )
            }
        }

        fun applyTemplateSelection(
            templateConfig: ModuleConfig,
            initiallySelected: Set<String>,
            selectedNow: Set<String>,
            notifyOnScopeError: Boolean = false,
        ) {
            val enabledPackages = selectedNow - initiallySelected
            val disabledPackages = initiallySelected - selectedNow
            if (enabledPackages.isEmpty() && disabledPackages.isEmpty()) return

            val prefs = PackageConfig.safePrefs
            val editor = prefs.edit()
            fun getSavedConfig(packageName: String): ModuleConfig {
                val config = prefs.getString(packageName, null)
                    ?.let { runCatching { ModuleConfig.fromJson(it) }.getOrNull() }
                    ?: ModuleConfig(packageName = packageName, enabled = false)
                config.packageName = packageName
                return config
            }

            disabledPackages.forEach { packageName ->
                val config = getSavedConfig(packageName)
                config.enabled = false
                editor.putString(packageName, config.toJson())
            }
            val scopeEnablePackages = enabledPackages.filterTo(mutableSetOf()) { packageName ->
                !getSavedConfig(packageName).enabled
            }
            enabledPackages.forEach { packageName ->
                val config = templateConfig.copy(packageName = packageName, enabled = true)
                editor.putString(packageName, config.toJson())
            }
            if (!editor.commit()) {
                if (notifyOnScopeError) {
                    GlobalSnackbarHost.showOnErrorByDismissPrevious(
                        ContextAmbient.current.getString(R.string.save_failed, "")
                    )
                }
                return
            }

            LauncherState.setAppsEnabled(enabledPackages, disabledPackages)
            PackageConfig.notifyConfigurationsChanged()

            val service = ContextAmbient.xposedService ?: run {
                if (notifyOnScopeError) {
                    GlobalSnackbarHost.showOnErrorByDismissPrevious(
                        ContextAmbient.current.getString(R.string.xposed_service_not_connected)
                    )
                }
                return
            }
            if (disabledPackages.isNotEmpty()) {
                runCatching { service.removeScope(disabledPackages.toList()) }
                    .onFailure {
                        if (notifyOnScopeError) {
                            GlobalSnackbarHost.showOnErrorByDismissPrevious(
                                ContextAmbient.current.getString(
                                    R.string.xposed_scope_sync_failed,
                                    it.message ?: it.toString(),
                                )
                            )
                        }
                    }
            }
            if (scopeEnablePackages.isNotEmpty()) {
                service.requestScope(
                    scopeEnablePackages.toList(),
                    object : XposedService.OnScopeEventListener {
                        override fun onScopeRequestApproved(approved: List<String>) = Unit

                        override fun onScopeRequestFailed(message: String) {
                            if (notifyOnScopeError) {
                                reportBatchScopeError(message)
                            }
                        }
                    },
                )
            }
        }

        private fun reportBatchScopeError(message: String) {
            GlobalSnackbarHost.showOnErrorByDismissPrevious(
                ContextAmbient.current.getString(R.string.xposed_scope_sync_failed, message)
            )
        }
    }


}

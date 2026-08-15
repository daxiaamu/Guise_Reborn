package com.houvven.guise.xposed.config

import com.houvven.guise.xposed.PackageConfig
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.Json

@Serializable
data class ModuleConfig(
    @Transient var packageName: String = "",
    var enabled: Boolean = true,
    var brand: String = "",
    var manufacturer: String = "",
    var model: String = "",
    var product: String = "",
    var device: String = "",
    var board: String = "",
    var hardware: String = "",
    var buildId: String = "",
    var androidVersion: String = "",
    var sdkInt: Int = -1,
    var densityDpi: Int = -1,
    var networkType: Int = HooksValue.NET_UNHOOK,
    var fingerPrint: String = "",
    var wifiSSID: String = "",
    var wifiBSSID: String = "",
    var wifiMacAddress: String = "",
    var simOperator: String = "",
    var simOperatorName: String = "",
    var simCountry: String = "",
    var visibleSimCount: Int = -1,
    var imei: String = "",
    var phoneNum: String = "",
    var androidId: String = "",
    var advertisingId: String = "",
    var lac: Int = -1,
    var cid: Int = -1,
    var language: String = "",
    var timeZone: String = "",
    var longitude: Double = -1.0,
    var latitude: Double = -1.0,
    var randomOffset: Boolean = false,
    var makeWifiLocationFail: Boolean = false,
    var makeCellLocationFail: Boolean = false,
    var gpuVendor: String = "",
    var gpuRenderer: String = "",
    var webViewUserAgent: String = "",
    var cameraCount: Int = -1,
    var hideExternalAudioDevices: Boolean = false,
    var versionCode: Int = -1,
    var versionName: String = "",
    var batteryLevel: Int = -1,
    var screenshotsFlag: Int = HooksValue.SCREENSHOTS_UNHOOK,
    var passContacts: Boolean = false,
    var passPhoto: Boolean = false,
    var passVideo: Boolean = false,
    var passAudio: Boolean = false,
    var passApplications: Boolean = false,
) {
    val isEnable: Boolean get() = enabled

    fun toJson() = json.encodeToString(serializer(), this)

    fun hasSameParameters(other: ModuleConfig): Boolean =
        copy(packageName = "", enabled = false) == other.copy(packageName = "", enabled = false)

    fun parameterSignature(): String =
        json.encodeToString(serializer(), copy(packageName = "", enabled = false))

    fun toModuleConfigState() = ModuleConfigState.of(this)

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun fromJson(value: String) = json.decodeFromString(serializer(), value)

        fun get(packageName: String): ModuleConfig {
            val config = PackageConfig.safePrefs.getString(packageName, null)?.let { fromJson(it) }
            config?.packageName = packageName
            return config ?: ModuleConfig(packageName = packageName, enabled = false)
        }

        fun getAllSaved(): List<ModuleConfig> =
            PackageConfig.safePrefs.all.mapNotNull { (packageName, value) ->
                val json = value as? String ?: return@mapNotNull null
                runCatching { fromJson(json) }.getOrNull()?.apply {
                    this.packageName = packageName
                }
            }
    }
}

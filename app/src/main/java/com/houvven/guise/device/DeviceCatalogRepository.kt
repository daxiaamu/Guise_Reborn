package com.houvven.guise.device

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class DeviceModel(
    val model: String,
    val name: String,
    val device: String = "",
    val code: String? = null,
    val alias: String? = null,
    val type: String,
    val versionName: String? = null,
)

@Serializable
data class DeviceBrand(
    val key: String,
    val displayName: String,
    val buildBrand: String,
    val manufacturer: String,
    val models: List<DeviceModel>,
)

@Serializable
data class DeviceCatalog(
    val schemaVersion: Int,
    val sourceRevision: String? = null,
    val brands: List<DeviceBrand>,
) {
    private val brandsByKey = brands.associateBy { it.key.lowercase() }

    fun brandByKey(key: String): DeviceBrand? = brandsByKey[key.lowercase()]

    fun brandForConfiguredValue(value: String): DeviceBrand? =
        brandByKey(value) ?: brands.firstOrNull {
            it.buildBrand.equals(value, ignoreCase = true) || it.displayName == value
        }
}

object DeviceCatalogRepository {
    private val json = Json { ignoreUnknownKeys = true }

    @Volatile
    private var cached: DeviceCatalog? = null

    suspend fun get(context: Context): DeviceCatalog = cached ?: withContext(Dispatchers.IO) {
        cached ?: synchronized(this@DeviceCatalogRepository) {
            cached ?: context.assets.open("devices.json").bufferedReader().use { reader ->
                json.decodeFromString<DeviceCatalog>(reader.readText())
                    .also { cached = it }
            }
        }
    }
}
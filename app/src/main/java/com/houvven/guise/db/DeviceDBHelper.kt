package com.houvven.guise.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.LruCache
import com.houvven.guise.constant.AppConfigKey
import java.io.Closeable
import java.io.File

class DeviceDBHelper(context: Context) : Closeable {

    private val store = SharedDeviceStore.get(context.applicationContext)

    fun getAllBrand(): Map<String, String> = store.getAllBrands()

    fun getDevicesByBrand(brand: String): List<Device> = store.getDevicesByBrand(brand)

    override fun close() = Unit
}

private object SharedDeviceStore {
    private const val DATABASE_VERSION = 3
    private const val DATABASE_FILE_NAME = "devices.db"
    private const val MAX_CACHED_DEVICES = 2_000

    private val lock = Any()
    private val deviceCache = object : LruCache<String, List<Device>>(MAX_CACHED_DEVICES) {
        override fun sizeOf(key: String, value: List<Device>): Int = value.size.coerceAtLeast(1)
    }

    @Volatile
    private var database: SQLiteDatabase? = null

    @Volatile
    private var brands: Map<String, String>? = null

    fun get(context: Context): SharedDeviceStore {
        ensureOpen(context)
        return this
    }

    fun getAllBrands(): Map<String, String> = brands ?: synchronized(lock) {
        brands ?: queryBrands(requireDatabase()).also { brands = it }
    }

    fun getDevicesByBrand(brand: String): List<Device> {
        val normalizedBrand = brand.trim().lowercase()
        if (normalizedBrand.isEmpty()) return emptyList()
        synchronized(deviceCache) {
            deviceCache.get(normalizedBrand)?.let { return it }
        }
        val devices = queryDevices(requireDatabase(), brand.trim())
        synchronized(deviceCache) {
            deviceCache.put(normalizedBrand, devices)
        }
        return devices
    }

    private fun ensureOpen(context: Context) {
        if (database?.isOpen == true) return
        synchronized(lock) {
            if (database?.isOpen == true) return
            val databaseFile = context.getDatabasePath(DATABASE_FILE_NAME)
            installBundledDatabaseIfNeeded(context, databaseFile)
            database = SQLiteDatabase.openDatabase(
                databaseFile.absolutePath,
                null,
                SQLiteDatabase.OPEN_READONLY,
            )
        }
    }

    private fun installBundledDatabaseIfNeeded(context: Context, databaseFile: File) {
        val installedVersion = AppConfigKey.mmkv.decodeInt(AppConfigKey.DEVICE_DB_VERSION, 0)
        if (installedVersion >= DATABASE_VERSION && databaseFile.isFile) return

        databaseFile.parentFile?.mkdirs()
        val temporaryFile = File(databaseFile.parentFile, "$DATABASE_FILE_NAME.tmp")
        temporaryFile.delete()
        try {
            context.assets.open(DATABASE_FILE_NAME).use { input ->
                temporaryFile.outputStream().buffered().use { output -> input.copyTo(output) }
            }
            check(temporaryFile.length() > 0L) { "Bundled device database is empty" }
            if (databaseFile.exists() && !databaseFile.delete()) {
                error("Unable to replace ${databaseFile.absolutePath}")
            }
            if (!temporaryFile.renameTo(databaseFile)) {
                temporaryFile.copyTo(databaseFile, overwrite = true)
                temporaryFile.delete()
            }
            AppConfigKey.mmkv.encode(AppConfigKey.DEVICE_DB_VERSION, DATABASE_VERSION)
        } finally {
            temporaryFile.delete()
        }
    }

    private fun requireDatabase(): SQLiteDatabase =
        requireNotNull(database?.takeIf { it.isOpen }) {
            "Device database is not open"
        }

    private fun queryBrands(db: SQLiteDatabase): Map<String, String> = buildMap {
        db.rawQuery(
            "select brand, brand_title from models group by brand order by brand_title",
            null,
        ).use { cursor ->
            while (cursor.moveToNext()) {
                put(cursor.getString(0), cursor.getString(1))
            }
        }
    }

    private fun queryDevices(db: SQLiteDatabase, brand: String): List<Device> = buildList {
        db.rawQuery(
            """
            select * from models
            where brand = ? collate nocase and model_name is not null and model_name != ''
              and dtype in ('mob', 'pad', 'tv', 'tv_hub', 'watch')
            group by model
            order by model_name, model
            """.trimIndent(),
            arrayOf(brand),
        ).use { cursor ->
            while (cursor.moveToNext()) {
                add(
                    Device(
                        brand = cursor.getString(cursor.getColumnIndexOrThrow("brand")),
                        brandTitle = cursor.getString(cursor.getColumnIndexOrThrow("brand_title")),
                        code = cursor.getString(cursor.getColumnIndexOrThrow("code")),
                        codeAlias = cursor.getString(cursor.getColumnIndexOrThrow("code_alias")),
                        dtype = cursor.getString(cursor.getColumnIndexOrThrow("dtype")),
                        model = cursor.getString(cursor.getColumnIndexOrThrow("model")),
                        modelName = cursor.getString(cursor.getColumnIndexOrThrow("model_name")),
                        verName = cursor.getString(cursor.getColumnIndexOrThrow("ver_name")),
                    )
                )
            }
        }
    }
}

package com.houvven.guise.xposed.hook

internal object VulkanPrivacyBridge {
    private const val LIBRARY_NAME = "guise_vulkan"
    private const val MAX_RENDERER_CHARS = 80

    private val libraryLoadResult by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        runCatching { System.loadLibrary(LIBRARY_NAME) }
    }

    fun configure(renderer: String): Result<Unit> {
        val normalized = normalizeRenderer(renderer)
        if (normalized.isEmpty()) return Result.success(Unit)
        return libraryLoadResult.mapCatching {
            nativeConfigureRenderer(normalized)
        }
    }

    internal fun normalizeRenderer(renderer: String): String =
        renderer.trim().take(MAX_RENDERER_CHARS)

    private external fun nativeConfigureRenderer(renderer: String)
}

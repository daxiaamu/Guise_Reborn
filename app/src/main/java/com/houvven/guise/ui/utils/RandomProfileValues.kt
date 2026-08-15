package com.houvven.guise.ui.utils

import kotlin.random.Random

internal data class GpuIdentity(
    val vendor: String,
    val renderer: String,
)

internal fun selectGpuIdentity(
    brand: String,
    api: Int,
    random: Random = Random.Default,
): GpuIdentity {
    val family = weightedGpuFamily(brand, api, random)
    val candidates = when (family) {
        GpuFamily.QUALCOMM -> qualcommCandidates(api)
        GpuFamily.ARM -> armCandidates(api)
        GpuFamily.IMAGINATION -> imaginationCandidates(api)
    }
    return candidates[random.nextInt(candidates.size)]
}

internal fun selectVisibleCameraCount(
    actualCount: Int,
    random: Random = Random.Default,
): Int {
    val count = actualCount.coerceIn(0, MAX_VISIBLE_CAMERAS)
    if (count == 0) return 0

    val totalWeight = count * (count + 1) / 2
    val ticket = random.nextInt(totalWeight) + 1
    var cumulative = 0
    for (visible in 1..count) {
        cumulative += visible
        if (ticket <= cumulative) return visible
    }
    return count
}

internal fun selectVisibleSimCount(
    maxModems: Int,
    mobileNetwork: Boolean,
    random: Random = Random.Default,
): Int {
    val max = maxModems.coerceIn(0, MAX_VISIBLE_SIMS)
    if (max == 0) return 0

    val choices = buildList {
        if (!mobileNetwork) add(0 to (1 shl max))
        for (count in 1..max) {
            add(count to (1 shl (max - count)))
        }
    }
    return weightedChoice(choices, random)
}

internal fun randomizeExternalAudioVisibility(random: Random = Random.Default): Boolean =
    random.nextInt(3) == 0

internal fun rewriteWebViewUserAgent(
    baseUserAgent: String,
    androidVersion: String,
    model: String,
): String {
    val engine = baseUserAgent.substringAfter(") ", "")
        .takeIf { it.contains("AppleWebKit/") && it.contains("Safari/") }
        ?: FALLBACK_WEBVIEW_ENGINE
    val safeVersion = androidVersion.trim().ifBlank { "10" }
        .replace(Regex("[^0-9A-Za-z._-]"), "")
        .ifBlank { "10" }
    val safeModel = model.trim()
        .replace(Regex("[();]"), "_")
        .take(64)
        .ifBlank { "Android Device" }
    return "Mozilla/5.0 (Linux; Android $safeVersion; $safeModel; wv) $engine"
}

private fun weightedGpuFamily(brand: String, api: Int, random: Random): GpuFamily {
    val normalized = brand.lowercase()
    val weights = when {
        normalized.contains("google") && api >= 31 -> listOf(
            GpuFamily.ARM to 8,
            GpuFamily.QUALCOMM to 2,
        )
        normalized.contains("huawei") || normalized.contains("honor") -> listOf(
            GpuFamily.ARM to 9,
            GpuFamily.QUALCOMM to 1,
        )
        normalized.contains("samsung") -> listOf(
            GpuFamily.ARM to 6,
            GpuFamily.QUALCOMM to 4,
        )
        normalized.containsAny(
            "xiaomi", "redmi", "poco", "oneplus", "oppo", "realme",
            "vivo", "iqoo", "motorola", "sony", "nothing", "asus",
        ) -> listOf(
            GpuFamily.QUALCOMM to 7,
            GpuFamily.ARM to 3,
        )
        api <= 30 -> listOf(
            GpuFamily.QUALCOMM to 6,
            GpuFamily.ARM to 3,
            GpuFamily.IMAGINATION to 1,
        )
        else -> listOf(
            GpuFamily.QUALCOMM to 6,
            GpuFamily.ARM to 4,
        )
    }
    return weightedChoice(weights, random)
}

private fun qualcommCandidates(api: Int): List<GpuIdentity> = when {
    api >= 35 -> listOf(
        GpuIdentity("Qualcomm", "Adreno (TM) 830"),
        GpuIdentity("Qualcomm", "Adreno (TM) 750"),
        GpuIdentity("Qualcomm", "Adreno (TM) 740"),
    )
    api >= 33 -> listOf(
        GpuIdentity("Qualcomm", "Adreno (TM) 740"),
        GpuIdentity("Qualcomm", "Adreno (TM) 730"),
        GpuIdentity("Qualcomm", "Adreno (TM) 725"),
        GpuIdentity("Qualcomm", "Adreno (TM) 660"),
    )
    api >= 31 -> listOf(
        GpuIdentity("Qualcomm", "Adreno (TM) 730"),
        GpuIdentity("Qualcomm", "Adreno (TM) 660"),
        GpuIdentity("Qualcomm", "Adreno (TM) 650"),
    )
    else -> listOf(
        GpuIdentity("Qualcomm", "Adreno (TM) 650"),
        GpuIdentity("Qualcomm", "Adreno (TM) 640"),
        GpuIdentity("Qualcomm", "Adreno (TM) 620"),
        GpuIdentity("Qualcomm", "Adreno (TM) 618"),
    )
}

private fun armCandidates(api: Int): List<GpuIdentity> = when {
    api >= 35 -> listOf(
        GpuIdentity("ARM", "Immortalis-G925"),
        GpuIdentity("ARM", "Immortalis-G720"),
        GpuIdentity("ARM", "Mali-G720"),
    )
    api >= 33 -> listOf(
        GpuIdentity("ARM", "Immortalis-G715"),
        GpuIdentity("ARM", "Mali-G715"),
        GpuIdentity("ARM", "Mali-G710"),
        GpuIdentity("ARM", "Mali-G610"),
    )
    api >= 31 -> listOf(
        GpuIdentity("ARM", "Mali-G710"),
        GpuIdentity("ARM", "Mali-G78"),
        GpuIdentity("ARM", "Mali-G68"),
    )
    else -> listOf(
        GpuIdentity("ARM", "Mali-G77"),
        GpuIdentity("ARM", "Mali-G76"),
        GpuIdentity("ARM", "Mali-G57"),
        GpuIdentity("ARM", "Mali-G52"),
    )
}

private fun imaginationCandidates(api: Int): List<GpuIdentity> =
    if (api >= 30) {
        listOf(
            GpuIdentity("Imagination Technologies", "PowerVR Rogue GM9446"),
            GpuIdentity("Imagination Technologies", "PowerVR Rogue GE8320"),
        )
    } else {
        listOf(
            GpuIdentity("Imagination Technologies", "PowerVR Rogue GE8320"),
            GpuIdentity("Imagination Technologies", "PowerVR Rogue GE8100"),
        )
    }

private fun <T> weightedChoice(values: List<Pair<T, Int>>, random: Random): T {
    val total = values.sumOf { it.second }
    var ticket = random.nextInt(total)
    values.forEach { (value, weight) ->
        if (ticket < weight) return value
        ticket -= weight
    }
    return values.last().first
}

private fun String.containsAny(vararg values: String): Boolean =
    values.any(::contains)

private enum class GpuFamily {
    QUALCOMM,
    ARM,
    IMAGINATION,
}

private const val MAX_VISIBLE_CAMERAS = 16
private const val MAX_VISIBLE_SIMS = 4
private const val FALLBACK_WEBVIEW_ENGINE =
    "AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/120.0.0.0 Mobile Safari/537.36"

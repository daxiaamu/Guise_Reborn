package com.houvven.ktx_xposed.logger

import java.nio.charset.StandardCharsets
import java.util.Base64

data class RuntimeLogEvent(
    val id: String,
    val timestamp: Long,
    val level: Char,
    val packageName: String,
    val processName: String,
    val category: String,
    val message: String,
    val stackTrace: String,
)

object RuntimeLogProtocol {
    const val PREFERENCES_NAME = "guise_runtime_logs_v2"
    const val DETAILED_LOGGING_KEY = "detailed_logging"
    const val DELIVERY_TOKEN_KEY = "delivery_token"
    const val DELIVERY_ACTION = "com.houvven.guise.action.APPEND_RUNTIME_LOGS"
    const val DELIVERY_PACKAGE = "com.houvven.guise"
    const val DELIVERY_RECEIVER = "com.houvven.guise.log.RuntimeLogReceiver"
    const val DELIVERY_EVENTS_EXTRA = "events"
    const val DELIVERY_TOKEN_EXTRA = "token"
    const val MAX_PENDING_EVENTS = 64
    const val MAX_DELIVERY_BATCH_SIZE = 50
    const val DELIVERY_DELAY_MS = 5_000L
    const val DELIVERY_RETRY_DELAY_MS = 30_000L

    private const val FORMAT_VERSION = "2"
    private val encoder = Base64.getUrlEncoder().withoutPadding()
    private val decoder = Base64.getUrlDecoder()

    fun encode(events: List<RuntimeLogEvent>): String = events.joinToString("\n") { event ->
        listOf(
            FORMAT_VERSION,
            encodeText(event.id),
            event.timestamp.toString(),
            event.level.toString(),
            encodeText(event.packageName),
            encodeText(event.processName),
            encodeText(event.category),
            encodeText(event.message),
            encodeText(event.stackTrace),
        ).joinToString("|")
    }

    fun decode(encoded: String?): List<RuntimeLogEvent> {
        if (encoded.isNullOrBlank()) return emptyList()
        return encoded.lineSequence().mapNotNull(::decodeLine).toList()
    }

    private fun decodeLine(line: String): RuntimeLogEvent? = runCatching {
        val fields = line.split('|', limit = 9)
        if (fields.size != 9 || fields[0] != FORMAT_VERSION) return null
        val level = fields[3].singleOrNull() ?: return null
        RuntimeLogEvent(
            id = decodeText(fields[1]),
            timestamp = fields[2].toLong(),
            level = level,
            packageName = decodeText(fields[4]),
            processName = decodeText(fields[5]),
            category = decodeText(fields[6]),
            message = decodeText(fields[7]),
            stackTrace = decodeText(fields[8]),
        )
    }.getOrNull()

    private fun encodeText(value: String): String =
        encoder.encodeToString(value.toByteArray(StandardCharsets.UTF_8))

    private fun decodeText(value: String): String =
        String(decoder.decode(value), StandardCharsets.UTF_8)
}

package com.hbv501g.forumapp.ui.util

import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeParseException

fun String.toRelativeTimeLabel(now: Instant = Instant.now()): String {
    val timestamp = trim()
    if (timestamp.isBlank()) return ""

    val instant = parseTimestamp(timestamp) ?: return timestamp
    val duration = Duration.between(instant, now)

    if (duration.isNegative) return "just now"

    val minutes = duration.toMinutes()
    val hours = duration.toHours()
    val days = duration.toDays()

    return when {
        minutes < 1 -> "just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        days < 7 -> "${days}d ago"
        days < 30 -> "${days / 7}w ago"
        days < 365 -> "${days / 30}mo ago"
        else -> "${days / 365}y ago"
    }
}

private fun parseTimestamp(value: String): Instant? {
    return parseInstant(value)
        ?: parseOffsetDateTime(value)
        ?: parseLocalDateTime(value)
}

private fun parseInstant(value: String): Instant? {
    return try {
        Instant.parse(value)
    } catch (_: DateTimeParseException) {
        null
    }
}

private fun parseOffsetDateTime(value: String): Instant? {
    return try {
        OffsetDateTime.parse(value).toInstant()
    } catch (_: DateTimeParseException) {
        null
    }
}

private fun parseLocalDateTime(value: String): Instant? {
    return try {
        LocalDateTime.parse(value).atZone(ZoneId.systemDefault()).toInstant()
    } catch (_: DateTimeParseException) {
        null
    }
}

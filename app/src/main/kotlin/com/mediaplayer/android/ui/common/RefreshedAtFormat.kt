package com.mediaplayer.android.ui.common

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Formats an ISO-8601 instant from the backend (e.g.
 * `Playlist.lastRefreshedAt`) into an Italian relative timestamp suitable
 * for surface labels like "oggi alle 04:03" / "ieri alle 04:05" /
 * "10 mag alle 04:01". Returns null when the input is blank or malformed
 * so callers can fall back to a generic placeholder.
 */
fun formatRefreshedAt(iso: String?): String? {
    if (iso.isNullOrBlank()) return null
    val instant = runCatching { Instant.parse(iso) }.getOrNull() ?: return null
    val zone = ZoneId.systemDefault()
    val zdt = instant.atZone(zone)
    val today = LocalDate.now(zone)
    val day = zdt.toLocalDate()
    val time = zdt.format(TIME_FMT)
    return when {
        day == today -> "oggi alle $time"
        day == today.minusDays(1) -> "ieri alle $time"
        else -> "${day.format(DATE_FMT)} alle $time"
    }
}

/**
 * Picks the most recent [Playlist.lastRefreshedAt] across [isoTimestamps]
 * and formats it via [formatRefreshedAt]. Returns null when no entry
 * carries a parseable timestamp — the For You header then falls back to
 * a generic schedule string.
 */
fun formatMostRecentRefreshedAt(isoTimestamps: Iterable<String?>): String? {
    val latest = isoTimestamps.asSequence()
        .mapNotNull { it?.takeIf(String::isNotBlank) }
        .mapNotNull { runCatching { Instant.parse(it) }.getOrNull() }
        .maxOrNull()
        ?: return null
    return formatRefreshedAt(latest.toString())
}

private val TIME_FMT = DateTimeFormatter.ofPattern("HH:mm", Locale.ITALIAN)
private val DATE_FMT = DateTimeFormatter.ofPattern("d MMM", Locale.ITALIAN)

/**
 * Compact day label — "Oggi" / "Ieri" / "10 mag" — for surfaces that have
 * little horizontal room (e.g. half-width MetaCards in the playlist
 * detail strip). The full "oggi alle 04:03" form belongs to
 * [formatRefreshedAt], used in the wider descriptive lines.
 */
fun formatRefreshedAtShort(iso: String?): String? {
    if (iso.isNullOrBlank()) return null
    val instant = runCatching { Instant.parse(iso) }.getOrNull() ?: return null
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now(zone)
    val day = instant.atZone(zone).toLocalDate()
    return when {
        day == today -> "Oggi"
        day == today.minusDays(1) -> "Ieri"
        else -> day.format(DATE_FMT)
    }
}

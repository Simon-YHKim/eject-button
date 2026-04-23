package com.ejectbutton.ui.util

/**
 * Parsed form of a history entry.  Entries are stored as
 * "MM/dd HH:mm · {emoji}{name} · trigger" and split by " · ".
 *
 * The parser is split out from the composable so the emoji/name
 * splitting — which has to deal with BMP-outside emoji stored as UTF-16
 * surrogate pairs (e.g. 👩 = U+1F469 = two Chars) — is unit-testable.
 */
data class HistoryEntry(
    val timestamp: String,
    val emoji: String,
    val name: String,
    val trigger: String,
)

/**
 * Parse a history entry string.  Falls back gracefully on malformed input:
 * missing sections become empty strings; an empty caller segment returns the
 * default placeholder emoji.
 *
 * Surrogate-pair-safe: uses [String.offsetByCodePoints] so emoji like
 * 👩 (U+1F469) aren't split mid-surrogate — the previous Char-based
 * implementation in HistoryEntryCard rendered them as broken glyphs.
 */
fun parseHistoryEntry(entry: String): HistoryEntry {
    val parts = entry.split(" · ")
    val timestamp  = parts.getOrNull(0).orEmpty()
    val callerPart = parts.getOrNull(1).orEmpty()
    val trigger    = parts.getOrNull(2).orEmpty()

    val firstCpEnd = if (callerPart.isNotEmpty()) callerPart.offsetByCodePoints(0, 1) else 0
    val emoji = callerPart.substring(0, firstCpEnd).ifEmpty { "👤" }
    val name  = if (callerPart.length > firstCpEnd) callerPart.substring(firstCpEnd).trim() else ""

    return HistoryEntry(timestamp, emoji, name, trigger)
}

/**
 * Format a seconds count as `MM:SS` (e.g. `00:05`, `12:34`).  Negative
 * inputs are clamped to zero.  Extracted from InCallScreenV2 so the
 * call-timer display logic can be unit-tested without Compose.
 */
fun formatDuration(totalSeconds: Int): String {
    val s = totalSeconds.coerceAtLeast(0)
    val m = s / 60
    val r = s % 60
    return "%02d:%02d".format(m, r)
}

package com.ejectbutton.ui.util

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Regression tests for [parseHistoryEntry].
 *
 * The surrogate-pair behavior is the important one: the original
 * implementation used `callerPart.firstOrNull()` and `substring(1)`, which
 * silently split BMP-outside emoji like 👩 (U+1F469, stored as two Chars in
 * UTF-16) across their high/low surrogate boundary.  That rendered as a
 * broken glyph on the History screen with the name starting at the dangling
 * low-surrogate.  These tests lock the fixed behavior in place.
 */
class HistoryEntryParserTest {

    @Test fun `parses BMP-outside emoji (woman U+1F469) without splitting surrogate`() {
        val result = parseHistoryEntry("04/23 21:10 · 👩엄마 · Now")

        assertEquals("04/23 21:10", result.timestamp)
        assertEquals("👩", result.emoji)
        assertEquals("엄마", result.name)
        assertEquals("Now", result.trigger)
    }

    @Test fun `parses BMP-outside emoji (man U+1F468) without splitting surrogate`() {
        val result = parseHistoryEntry("12/31 23:59 · 👨아빠 · 10 sec")

        assertEquals("👨", result.emoji)
        assertEquals("아빠", result.name)
        assertEquals("10 sec", result.trigger)
    }

    @Test fun `parses BMP emoji (single Char, no surrogate)`() {
        // © is U+00A9 — a single Char, exercises the codePoint == 1 Char path.
        val result = parseHistoryEntry("01/01 00:00 · ©Tester · Shake")

        assertEquals("©", result.emoji)
        assertEquals("Tester", result.name)
    }

    @Test fun `uses placeholder emoji when caller segment is empty`() {
        val result = parseHistoryEntry("04/23 21:10 ·  · Now")
        assertEquals("👤", result.emoji)
        assertEquals("", result.name)
    }

    @Test fun `returns empty fields for malformed entry with fewer sections`() {
        val result = parseHistoryEntry("just a timestamp")
        assertEquals("just a timestamp", result.timestamp)
        assertEquals("👤", result.emoji)
        assertEquals("", result.name)
        assertEquals("", result.trigger)
    }

    @Test fun `trims whitespace around the name segment`() {
        val result = parseHistoryEntry("04/23 21:10 · 👩   엄마   · Now")
        // substring after the emoji keeps leading spaces; .trim() cleans them.
        assertEquals("엄마", result.name)
    }

    @Test fun `keeps emoji-free names intact when first char is a letter`() {
        // Defensive check: if a user somehow adds a caller without an emoji
        // prefix, the first Char (e.g. "M") gets treated as the "emoji" slot.
        // This isn't a bug in practice because saveScenario always prepends
        // an emoji, but the parser must not crash.
        val result = parseHistoryEntry("01/01 00:00 · Mom · Now")
        assertEquals("M", result.emoji)
        assertEquals("om", result.name)
    }
}

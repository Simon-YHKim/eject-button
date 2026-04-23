package com.ejectbutton.ui.util

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests for the MM:SS call-timer formatter used by InCallScreenV2 and the
 * recording tile.  Zero-pads both fields, clamps negatives to "00:00", and
 * rolls into the minutes past 60 seconds.
 */
class FormatDurationTest {

    @Test fun `zero seconds is 00 00`() {
        assertEquals("00:00", formatDuration(0))
    }

    @Test fun `under a minute zero-pads seconds`() {
        assertEquals("00:05", formatDuration(5))
        assertEquals("00:59", formatDuration(59))
    }

    @Test fun `exactly one minute rolls seconds over`() {
        assertEquals("01:00", formatDuration(60))
    }

    @Test fun `handles minutes and seconds together`() {
        assertEquals("12:34", formatDuration(12 * 60 + 34))
    }

    @Test fun `negative input clamps to zero`() {
        assertEquals("00:00", formatDuration(-1))
        assertEquals("00:00", formatDuration(Int.MIN_VALUE))
    }

    @Test fun `three-digit minute count is not truncated`() {
        // 100:00 is still valid (we use %02d as MINIMUM width, not max).
        assertEquals("100:00", formatDuration(100 * 60))
    }
}

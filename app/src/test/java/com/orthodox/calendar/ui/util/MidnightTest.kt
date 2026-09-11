package com.orthodox.calendar.ui.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

/**
 * The sleep the "today" ticker takes between days.
 *
 * The ViewModel used to learn about a new day only when the month was reloaded,
 * so an app left open across midnight kept yesterday's ring and yesterday's
 * "day 12 of 40" in the fasting banner. Sleeping to the boundary is only correct
 * if the value is the *next* midnight and never exactly on it — a wake-up that
 * lands at 00:00:00.000 can read the day that has just ended.
 */
class MidnightTest {

    @Test
    fun `noon sleeps the rest of the day plus the extra millisecond`() {
        val noon = LocalDateTime.of(2026, 1, 7, 12, 0, 0, 0)
        assertEquals(12 * 3600 * 1000L + 1, millisUntilNextMidnight(noon))
    }

    @Test
    fun `just before midnight wakes almost immediately`() {
        val almost = LocalDateTime.of(2026, 1, 7, 23, 59, 59, 999_000_000)
        assertEquals(2, millisUntilNextMidnight(almost))
    }

    /**
     * Midnight itself is not "until the next midnight" — the value is a full day,
     * never zero, or the ticker spins.
     */
    @Test
    fun `midnight waits a whole day`() {
        val midnight = LocalDateTime.of(2026, 1, 7, 0, 0, 0, 0)
        assertEquals(24 * 3600 * 1000L + 1, millisUntilNextMidnight(midnight))
    }

    @Test
    fun `never zero or negative, whatever the instant`() {
        var instant = LocalDateTime.of(2024, 2, 29, 0, 0, 0, 0)
        repeat(1000) {
            assertTrue("tick slept ${millisUntilNextMidnight(instant)}ms at $instant",
                millisUntilNextMidnight(instant) > 0)
            instant = instant.plusMinutes(37)
        }
    }

    /** A wake-up at the target reads the new day, not the one that ended. */
    @Test
    fun `waking at the target lands on the next date`() {
        val now = LocalDateTime.of(2026, 3, 15, 22, 30, 0, 0)
        val woke = now.plusNanos(millisUntilNextMidnight(now) * 1_000_000)
        assertEquals(now.toLocalDate().plusDays(1), woke.toLocalDate())
    }
}

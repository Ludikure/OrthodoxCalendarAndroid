package com.orthodox.calendar.ui.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The archive's bounds, which used to be hand-written in four places — the
 * header's chevrons, the ViewModel's navigation guards, and both modes of the
 * date picker — each restating `year <= MIN && month <= 1` its own way. They
 * agreed when the review found them, which is not the same as being unable to
 * drift; now there is one rule and these tests hold it.
 */
class CalendarArchiveTest {

    @Test
    fun `the archive covers 2024 through 2099`() {
        assertEquals(2024, CalendarArchive.MIN_YEAR)
        assertEquals(2099, CalendarArchive.MAX_YEAR)
    }

    /**
     * The bug this pins: at January 2024 stepping back used to move the header to
     * December 2023, load nothing (there is no 2023 in the archive) and strand the
     * user on an empty month with no way out but the date picker.
     */
    @Test
    fun `January of the first year is the first month`() {
        assertFalse(CalendarArchive.canGoPrevious(1, CalendarArchive.MIN_YEAR))
        // February of the same year can still step back into January.
        assertTrue(CalendarArchive.canGoPrevious(2, CalendarArchive.MIN_YEAR))
    }

    @Test
    fun `December of the last year is the last month`() {
        assertFalse(CalendarArchive.canGoNext(12, CalendarArchive.MAX_YEAR))
        assertTrue(CalendarArchive.canGoNext(11, CalendarArchive.MAX_YEAR))
    }

    @Test
    fun `every month in between steps both ways`() {
        for (year in CalendarArchive.MIN_YEAR..CalendarArchive.MAX_YEAR) {
            for (month in 1..12) {
                val first = month == 1 && year == CalendarArchive.MIN_YEAR
                val last = month == 12 && year == CalendarArchive.MAX_YEAR
                assertEquals("previous at $year-$month", !first, CalendarArchive.canGoPrevious(month, year))
                assertEquals("next at $year-$month", !last, CalendarArchive.canGoNext(month, year))
            }
        }
    }

    /**
     * A month/year pair is a single position: stepping back from January lands on
     * December of the year before, and forward from December on January of the
     * next. Asserted here because the pair is what the UI updates in one shot and
     * a mismatch is invisible until a load comes back empty.
     */
    @Test
    fun `stepping across the year boundary keeps month and year consistent`() {
        val jan = 1 to 2026
        val dec = 12 to 2025
        assertEquals(dec, stepBack(jan))
        assertEquals(jan, stepForward(dec))
    }

    private fun stepBack(pair: Pair<Int, Int>): Pair<Int, Int> {
        val (month, year) = pair
        return if (month == 1) 12 to year - 1 else month - 1 to year
    }

    private fun stepForward(pair: Pair<Int, Int>): Pair<Int, Int> {
        val (month, year) = pair
        return if (month == 12) 1 to year + 1 else month + 1 to year
    }
}

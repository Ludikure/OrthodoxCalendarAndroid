package com.orthodox.calendar.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * The fasting-season spans shown as "day 12 of 40".
 *
 * The data tags each day with a bare code (`great_lent`); the app has to
 * reconstruct the run it belongs to — where it starts, how long it is, and
 * whether the run is even fully visible. A run is broken by a gap in dates or a
 * change of code, and a run that touches the edge of the loaded data is *not*
 * complete, so the banner must not claim a day count it has not seen.
 */
class FastingPeriodSpansTest {

    private val names = mapOf("Great Lent" to "Крсни пост", "Nativity Fast" to "Бадњи пост")

    private fun day(date: String, period: String? = null) = CalendarDay(
        gregorianDate = date,
        julianDate = date.substring(5),
        dayOfWeek = 0,
        paschaDistance = 0,
        feasts = emptyList(),
        fasting = FastingInfo(type = "dryEating", label = "", explanation = ""),
        fastingPeriod = period
    )

    private fun daysOf(start: LocalDate, count: Int, period: String) =
        (0 until count).map { day(start.plusDays(it.toLong()).toString(), period) }

    @Test
    fun `a contiguous run gets one span with 1-based day numbers`() {
        val days = daysOf(LocalDate.of(2026, 2, 23), 5, "great_lent")
        val spans = FastingPeriods.computeSpans(padding(days), names)

        val first = spans["2026-02-23"]!!
        assertEquals("great_lent", first.code)
        assertEquals("Крсни пост", first.displayName)
        assertEquals(LocalDate.of(2026, 2, 23), first.start)
        assertEquals(LocalDate.of(2026, 2, 27), first.end)
        assertEquals(5, first.total)
        for (expected in 1..5) {
            val date = LocalDate.of(2026, 2, 22 + expected).toString()
            assertEquals("day index at $date", expected, spans[date]?.dayIndex)
            assertEquals("total at $date", 5, spans[date]?.total)
        }
        assertTrue("a run inside the data is complete", first.complete)
    }

    /** A missing day is a broken fast, not the same season continuing. */
    @Test
    fun `a gap in the dates starts a new span`() {
        val days = listOf(
            day("2026-02-23", "great_lent"),
            day("2026-02-24", "great_lent"),
            day("2026-02-26", "great_lent")
        )
        val spans = FastingPeriods.computeSpans(padding(days), names)
        assertEquals(2, spans["2026-02-23"]?.total)
        assertEquals(1, spans["2026-02-26"]?.total)
    }

    @Test
    fun `a change of period code starts a new span`() {
        val days = listOf(
            day("2026-02-23", "great_lent"),
            day("2026-02-24", "great_lent"),
            day("2026-02-25", "apostles_fast")
        )
        val spans = FastingPeriods.computeSpans(padding(days), names)
        assertEquals("great_lent", spans["2026-02-23"]?.code)
        assertEquals(2, spans["2026-02-23"]?.total)
        assertEquals("apostles_fast", spans["2026-02-25"]?.code)
        assertEquals(1, spans["2026-02-25"]?.total)
    }

    /**
     * The Nativity Fast runs Nov 28 – Jan 6 and the data is per year. A run that
     * reaches the first or last date available may continue in a year that was
     * never loaded, so "day 3 of 40" would be a lie — the banner shows the name
     * alone in that case.
     */
    @Test
    fun `a run touching the edge of the data is incomplete`() {
        val days = daysOf(LocalDate.of(2026, 2, 23), 3, "great_lent")
        // No padding: the run starts on the earliest date the caller has.
        val spans = FastingPeriods.computeSpans(days, names)
        assertFalse(spans["2026-02-23"]!!.complete)
        assertFalse(spans["2026-02-25"]!!.complete)
    }

    /** The caller passes a year in whatever order it was decoded. */
    @Test
    fun `input order does not matter`() {
        val days = padding(daysOf(LocalDate.of(2026, 2, 23), 4, "great_lent")).shuffled()
        val spans = FastingPeriods.computeSpans(days, names)
        assertEquals(4, spans["2026-02-23"]?.total)
        assertEquals(1, spans["2026-02-23"]?.dayIndex)
        assertEquals(4, spans["2026-02-26"]?.dayIndex)
    }

    @Test
    fun `days outside a season carry no span`() {
        val days = padding(listOf(day("2026-06-01", null), day("2026-06-02", null)))
        val spans = FastingPeriods.computeSpans(days, names)
        assertTrue(spans.isEmpty())
        assertNull(spans["2026-06-01"])
    }

    /** An unknown code still renders — as itself rather than a wrong name. */
    @Test
    fun `an unmapped code falls back to a humanised version of the code`() {
        val days = padding(listOf(day("2026-02-23", "some_new_fast")))
        val spans = FastingPeriods.computeSpans(days, names)
        assertEquals("Some New Fast", spans["2026-02-23"]?.displayName)
    }

    /**
     * The Nativity Fast runs 28 Nov – 6 Jan, so `12-31` and `01-01` carry the same
     * code in every year of the data and the season straddles the year boundary
     * annually. `seasonDays` therefore merges the adjacent year; this is what that
     * merge buys — one run, continuous day numbers, and a badge on 1 January. A
     * single-year list splits it into two short runs, which is the wrong day count
     * the banner used to show.
     */
    @Test
    fun `a season that crosses the year boundary is one run`() {
        val days = padding(daysOf(LocalDate.of(2025, 12, 29), 9, "nativity_fast"))
        val spans = FastingPeriods.computeSpans(days, names)

        assertEquals(3, spans["2025-12-31"]?.dayIndex)
        assertEquals("1 January continues the December run",
            4, spans["2026-01-01"]?.dayIndex)
        assertEquals(9, spans["2026-01-01"]?.total)
        assertEquals(spans["2025-12-31"]?.start, spans["2026-01-01"]?.start)
        assertEquals(spans["2025-12-31"]?.end, spans["2026-01-01"]?.end)
    }

    /** One day either side, so the runs under test are not at the window edges. */
    private fun padding(days: List<CalendarDay>): List<CalendarDay> {
        val first = LocalDate.parse(days.minOf { it.gregorianDate }).minusDays(1)
        val last = LocalDate.parse(days.maxOf { it.gregorianDate }).plusDays(1)
        return listOf(day(first.toString(), null)) + days + day(last.toString(), null)
    }
}

package com.orthodox.calendar.widget

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.time.LocalDate

/**
 * What the widget draws for a day: the snapshot's entry, or nothing (the
 * "open the app" placeholder) when the app has not written one that reaches it.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WidgetStateTest {
    private val context = RuntimeEnvironment.getApplication()

    @After
    fun clean() {
        WidgetSnapshot.file(context).delete()
    }

    private fun day(date: String) = WidgetSnapshot.Day(
        date = date, weekday = "Недеља", dateLabel = "27 Септембар", primary = "Крстовдан",
        secondary = emptyList(), isGreatFeast = true, fastingType = "hotWithOil",
        fastingLabel = "Уље дозвољено", fastingAbbrev = "уље"
    )

    @Test
    fun `no snapshot shows the placeholder`() {
        assertNull(WidgetState.load(context, LocalDate.parse("2026-09-27")).day)
    }

    @Test
    fun `picks today's entry and keeps the snapshot's language`() {
        val snapshot = WidgetSnapshot(
            language = "ru", generatedAt = "2026-09-27T00:00:00Z",
            days = listOf(day("2026-09-27"), day("2026-09-28"))
        )
        WidgetSnapshot.write(snapshot, WidgetSnapshot.file(context))

        val today = WidgetState.load(context, LocalDate.parse("2026-09-27"))
        assertEquals("ru", today.language)
        assertEquals("2026-09-27", today.day?.date)
        // The next midnight moves on without the app.
        assertEquals("2026-09-28", WidgetState.load(context, LocalDate.parse("2026-09-28")).day?.date)
        // Past the snapshot's end: the placeholder, never another day's saints.
        assertNull(WidgetState.load(context, LocalDate.parse("2026-09-29")).day)
        assertNull(WidgetState.load(context, LocalDate.parse("2026-09-26")).day)
    }
}

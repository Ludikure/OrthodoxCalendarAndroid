package com.orthodox.calendar.ui.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.util.Locale

/**
 * The `gregorianDate` key format.
 *
 * Every lookup in this app — the "today" ring, the detail route, the fasting
 * season map — is a string comparison against a key from the data files, so the
 * one thing that matters is that the format never depends on the device.
 */
class IsoDateTest {

    private fun withLocale(tag: String, block: () -> Unit) {
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag(tag))
            block()
        } finally {
            Locale.setDefault(previous)
        }
    }

    @Test
    fun `the key format is the same under every device locale`() {
        val date = LocalDate.of(2026, 1, 7)
        for (tag in listOf("en-US", "sr-RS", "ru-RU", "th-TH-u-nu-thai", "fa-IR", "ar-EG", "tr-TR")) {
            withLocale(tag) {
                assertEquals("key format moved with locale $tag", "2026-01-07", date.toIsoDate())
            }
        }
    }

    @Test
    fun `round trips`() {
        val date = LocalDate.of(2099, 12, 31)
        assertEquals(date, parseIsoDate(date.toIsoDate()))
    }

    @Test
    fun `null and junk parse to null rather than throwing`() {
        assertNull(parseIsoDate(null))
        assertNull(parseIsoDate(""))
        assertNull(parseIsoDate("07.01.2026"))
        assertNull(parseIsoDate("not a date"))
        assertNull(parseIsoDate("2026-13-01"))
    }

    /**
     * The route argument is user-reachable (a deep link, a restored back stack),
     * so a malformed date has to be a plain "no such day", never an exception on
     * the way to the screen.
     */
    @Test
    fun `a date the archive cannot name still parses or fails cleanly`() {
        assertEquals(LocalDate.of(2024, 1, 1), parseIsoDate("2024-01-01"))
        assertEquals(LocalDate.of(2099, 12, 31), parseIsoDate("2099-12-31"))
    }
}

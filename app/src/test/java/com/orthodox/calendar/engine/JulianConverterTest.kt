package com.orthodox.calendar.engine

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class JulianConverterTest {

    @Test
    fun `offset is 13 days for the supported era`() {
        assertEquals(13, JulianConverter.OFFSET)
    }

    @Test
    fun `julianComponents subtracts the offset`() {
        // 2026-01-14 Gregorian is 2026-01-01 Julian (Old New Year).
        val (month, day) = JulianConverter.julianComponents(LocalDate.of(2026, 1, 14))
        assertEquals(1, month)
        assertEquals(1, day)
    }

    @Test
    fun `julianDisplayString formats as dd slash MM`() {
        assertEquals("01/01", JulianConverter.julianDisplayString(LocalDate.of(2026, 1, 14)))
    }

    @Test
    fun `gregorianDate round-trips with julianComponents`() {
        val greg = JulianConverter.gregorianDate(1, 1, 2026)
        assertEquals(LocalDate.of(2026, 1, 14), greg)
        val (m, d) = JulianConverter.julianComponents(greg)
        assertEquals(1, m)
        assertEquals(1, d)
    }

    @Test
    fun `gregorianDate normalizes day overflow across month end`() {
        // Julian April 25 (+13) rolls into May.
        assertEquals(LocalDate.of(2026, 5, 8), JulianConverter.gregorianDate(4, 25, 2026))
    }
}

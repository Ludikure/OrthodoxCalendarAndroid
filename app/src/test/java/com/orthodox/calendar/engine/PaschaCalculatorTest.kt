package com.orthodox.calendar.engine

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class PaschaCalculatorTest {

    @Test
    fun `pascha matches known Gregorian Orthodox Easter dates`() {
        assertEquals(LocalDate.of(2024, 5, 5), PaschaCalculator.pascha(2024))
        assertEquals(LocalDate.of(2025, 4, 20), PaschaCalculator.pascha(2025))
        assertEquals(LocalDate.of(2026, 4, 12), PaschaCalculator.pascha(2026))
        assertEquals(LocalDate.of(2027, 5, 2), PaschaCalculator.pascha(2027))
    }

    @Test
    fun `paschaDistance is zero on Pascha and signed around it`() {
        val pascha2026 = PaschaCalculator.pascha(2026)
        assertEquals(0, PaschaCalculator.paschaDistance(pascha2026, 2026))
        assertEquals(7, PaschaCalculator.paschaDistance(pascha2026.plusDays(7), 2026))
        assertEquals(-3, PaschaCalculator.paschaDistance(pascha2026.minusDays(3), 2026))
    }
}

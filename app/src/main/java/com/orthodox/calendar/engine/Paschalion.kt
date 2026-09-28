package com.orthodox.calendar.engine

import java.time.LocalDate

/**
 * Orthodox Pascha for a year, computed rather than read from the data.
 *
 * The calendar screens take Pascha from the year files (`paschaDistance`), but
 * slava reminders are scheduled for years that may not be loaded — or even
 * downloaded — so a moveable slava (Lazarus Saturday, Spasovdan) needs its date
 * without them. `SlavaTest` checks this against every bundled year.
 *
 * Mirror of `OrthodoxCalendar/Engine/Paschalion.swift`.
 */
object Paschalion {
    /**
     * Pascha in the civil (Gregorian) calendar: Meeus's Julian algorithm plus the
     * 13-day offset, which holds from 1900-03-01 to 2100-02-28 — the whole span
     * the app serves (2024–2099).
     */
    fun pascha(year: Int): LocalDate {
        val a = year % 4
        val b = year % 7
        val c = year % 19
        val d = (19 * c + 15) % 30
        val e = (2 * a + 4 * b - d + 34) % 7
        val month = (d + e + 114) / 31
        val day = (d + e + 114) % 31 + 1
        return ChurchDates.gregorian(year, month, day)
    }
}

/** Julian ↔ civil date conversion for 1900–2099, where the offset is 13 days. */
object ChurchDates {
    fun isJulianLeap(year: Int): Boolean = year % 4 == 0

    /**
     * The civil date of Julian `year-month-day`. Julian February 29 in a year
     * without one is kept on February 28, as the Church does.
     */
    fun gregorian(julianYear: Int, month: Int, day: Int): LocalDate {
        val d = if (month == 2 && day == 29 && !isJulianLeap(julianYear)) 28 else day
        // The Julian date's labels read as a proleptic civil date, then shifted.
        // Julian Feb 29 of a Julian leap year that is not a civil leap year
        // (2100) is outside the app's range; LocalDate would reject it.
        return LocalDate.of(julianYear, month, d).plusDays(13)
    }

    /** The Julian month and day of a civil date. */
    fun julian(date: LocalDate): Pair<Int, Int> {
        val shifted = date.minusDays(13)
        return shifted.monthValue to shifted.dayOfMonth
    }
}

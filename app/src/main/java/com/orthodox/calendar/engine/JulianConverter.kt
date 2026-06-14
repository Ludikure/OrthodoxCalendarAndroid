package com.orthodox.calendar.engine

import java.time.LocalDate

/**
 * Julian ↔ Gregorian date conversion.
 *
 * Mirror of iOS `Engine/JulianConverter.swift`. Keep [OFFSET] and behaviour
 * identical across platforms (see PARITY.md).
 */
object JulianConverter {
    /** Julian-to-Gregorian offset for years 1900–2099. */
    const val OFFSET = 13

    /** Returns the Julian month and day for a given Gregorian date. */
    fun julianComponents(date: LocalDate): Pair<Int, Int> {
        val julian = date.minusDays(OFFSET.toLong())
        return julian.monthValue to julian.dayOfMonth
    }

    /** Formatted Julian date string "dd/MM". */
    fun julianDisplayString(date: LocalDate): String {
        val (month, day) = julianComponents(date)
        return "%02d/%02d".format(day, month)
    }

    /** Convert a Julian month/day to a Gregorian date for a given year. */
    fun gregorianDate(julianMonth: Int, julianDay: Int, year: Int): LocalDate =
        LocalDate.of(year, julianMonth, 1).plusDays((julianDay - 1 + OFFSET).toLong())
}

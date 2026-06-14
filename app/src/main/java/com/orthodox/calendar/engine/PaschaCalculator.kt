package com.orthodox.calendar.engine

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Computes the date of Pascha (Orthodox Easter).
 *
 * Mirror of iOS `Engine/PaschaCalculator.swift`: the Meeus Julian algorithm
 * plus the Julian-to-Gregorian offset (see PARITY.md).
 */
object PaschaCalculator {
    /** The Gregorian date of Pascha for a given year. */
    fun pascha(year: Int): LocalDate {
        val a = year % 19
        val b = year % 4
        val c = year % 7
        val d = (19 * a + 15) % 30
        val e = (2 * b + 4 * c + 6 * d + 6) % 7

        val julianMonth: Int
        val julianDay: Int
        if (d + e < 10) {
            julianMonth = 3
            julianDay = 22 + d + e
        } else {
            julianMonth = 4
            julianDay = d + e - 9
        }

        // Convert the Julian date to Gregorian (handles day overflow normalization).
        return JulianConverter.gregorianDate(julianMonth, julianDay, year)
    }

    /** Distance in days from Pascha for a given date. */
    fun paschaDistance(date: LocalDate, year: Int): Int =
        ChronoUnit.DAYS.between(pascha(year), date).toInt()
}

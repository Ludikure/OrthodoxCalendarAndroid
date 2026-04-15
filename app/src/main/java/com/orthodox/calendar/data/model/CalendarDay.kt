package com.orthodox.calendar.data.model

import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Serializable
data class CalendarDay(
    val gregorianDate: String,            // "2026-01-07"
    val julianDate: String,               // "12-25"
    val dayOfWeek: Int,                   // Python convention: 0=Mon..6=Sun
    val paschaDistance: Int,
    val feasts: List<Feast>,
    val liturgicalPeriod: String? = null,
    val weekLabel: String? = null,
    val greatFeast: String? = null,
    val fasting: FastingInfo,
    val readings: List<ScriptureReading> = emptyList(),
    val reflection: Reflection? = null,
    val saintBios: List<SaintBio>? = null,
    val fastingPeriod: String? = null,
    val isFastFreeWeek: Boolean? = null
) {
    /** The primary feast for this day (first feast with displayRole "primary") */
    val primaryFeast: Feast?
        get() = feasts.firstOrNull { it.displayRole == "primary" }

    /** Secondary feasts (displayRole == "secondary") */
    val secondaryFeasts: List<Feast>
        get() = feasts.filter { it.displayRole == "secondary" }

    /** Tertiary feasts (displayRole == "tertiary") */
    val tertiaryFeasts: List<Feast>
        get() = feasts.filter { it.displayRole == "tertiary" }

    /** Whether this day is a Sunday (Python convention: 6=Sun) */
    val isSunday: Boolean
        get() = dayOfWeek == 6

    /** Whether this day is a Saturday (Python convention: 5=Sat) */
    val isSaturday: Boolean
        get() = dayOfWeek == 5

    /** Converts Python weekday (0=Mon..6=Sun) to localization array index (0=Sun..6=Sat) */
    val weekdayIndex: Int
        get() = (dayOfWeek + 1) % 7

    /** The day number extracted from julianDate string "MM-DD" */
    val julianDay: Int
        get() {
            val lastDash = julianDate.lastIndexOf('-')
            if (lastDash < 0) return 0
            return julianDate.substring(lastDash + 1).toIntOrNull() ?: 0
        }

    /** The day-of-month number extracted from gregorianDate */
    val gregorianDay: Int
        get() {
            val lastDash = gregorianDate.lastIndexOf('-')
            if (lastDash < 0) return 0
            return gregorianDate.substring(lastDash + 1).toIntOrNull() ?: 0
        }

    /** The month number extracted from gregorianDate */
    val gregorianMonth: Int
        get() {
            val parts = gregorianDate.split("-")
            if (parts.size < 2) return 0
            return parts[1].toIntOrNull() ?: 0
        }

    /** Whether this day has a great feast */
    val isGreatFeast: Boolean
        get() = greatFeast != null

    /** Parsed LocalDate from gregorianDate string */
    val date: LocalDate?
        get() = try {
            LocalDate.parse(gregorianDate, dateFormatter)
        } catch (_: Exception) {
            null
        }

    companion object {
        private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    }
}

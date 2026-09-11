package com.orthodox.calendar.data.repository

import com.orthodox.calendar.data.model.CalendarDay
import com.orthodox.calendar.data.model.CalendarFile

/**
 * What the calendar screens need from a year of data.
 *
 * [CalendarRepository] is the only production implementation; the interface
 * exists so the ViewModel's load state machine — which month is on screen, what
 * gets cleared while a new one loads, what "could not resolve this day" means —
 * can be driven by a fake in tests instead of by a real repository reading real
 * assets. That state machine has caused most of this app's user-visible bugs,
 * and none of it was testable while it needed a `Context`.
 */
interface YearSource {

    sealed class LoadError : Exception() {
        /** No data exists for this locale/year. */
        object NotFound : LoadError()
        /** Connectivity problem; retry may succeed. */
        object Offline : LoadError()
    }

    /** Days for a single month; throws [LoadError] when the year can't load. */
    suspend fun loadMonth(locale: String, year: Int, month: Int): List<CalendarDay>

    /**
     * The whole year. [allowNetwork] `false` stops before the network and
     * throws [LoadError.NotFound] rather than downloading.
     */
    suspend fun load(locale: String, year: Int, allowNetwork: Boolean = true): CalendarFile
}

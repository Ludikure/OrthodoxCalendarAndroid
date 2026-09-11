package com.orthodox.calendar.ui.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * `yyyy-MM-dd`, the format every calendar key in this app uses.
 *
 * Pinned to [Locale.ROOT], matching the formatter `CalendarDay` already pins, so
 * the key format has one definition instead of several and none of them can
 * move with the device locale. `LocalDate.toString()` — what this replaces at
 * the call sites — is itself locale-independent, so this is pinning rather than
 * a live bug: `DateTimeFormatter.ofPattern("yyyy-MM-dd")` takes the default
 * locale, and a formatter built that way (or given a non-ASCII
 * [java.time.format.DecimalStyle]) produces digits that match no `gregorianDate`
 * in the data files. Cheap insurance against the day someone reaches for the
 * unpinned form.
 */
fun LocalDate.toIsoDate(): String = format(isoDateFormatter)

/** Parses [toIsoDate] output, or null when [text] is not a calendar date. */
fun parseIsoDate(text: String?): LocalDate? = try {
    if (text == null) null else LocalDate.parse(text, isoDateFormatter)
} catch (_: Exception) {
    null
}

private val isoDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ROOT)

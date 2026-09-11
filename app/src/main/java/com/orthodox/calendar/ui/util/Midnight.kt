package com.orthodox.calendar.ui.util

import java.time.Duration
import java.time.LocalDateTime

/**
 * Milliseconds from [now] until the next local midnight, plus one so a wake-up
 * never lands exactly on the boundary and reads the day that just ended.
 *
 * The ViewModel used to learn about a new day only when the month was reloaded,
 * which left the "today" ring and the fasting banner's day count pointing at
 * yesterday for however long the app sat open across midnight — a normal thing
 * for an app people check after Vespers. Sleeping to the boundary instead of
 * polling costs one coroutine and no work until then.
 *
 * A sleep measured with this is not enough on its own: a coroutine `delay` on
 * the main thread runs on uptime, which stops in deep sleep, so the ViewModel
 * also re-reads the date on every ON_START (`CalendarViewModel.refreshToday`).
 */
fun millisUntilNextMidnight(now: LocalDateTime): Long {
    val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay()
    return Duration.between(now, nextMidnight).toMillis() + 1
}

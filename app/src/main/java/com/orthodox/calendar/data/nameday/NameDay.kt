package com.orthodox.calendar.data.nameday

import com.orthodox.calendar.data.model.CalendarDay
import com.orthodox.calendar.data.slava.SlavaAnchor
import com.orthodox.calendar.data.slava.SlavaDay
import com.orthodox.calendar.engine.ChurchDates
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID

/**
 * A span of church dates and the weekday a moving commemoration keeps inside
 * it: "the Sunday after the Nativity" is Sunday between Julian 26 and 31
 * December. [weekday] follows the data's convention, 0 = Monday … 6 = Sunday
 * (`CalendarDay.dayOfWeek`); [first]/[last] are Julian "MM-DD", inclusive.
 */
@Serializable
data class NameDayWindow(val weekday: Int, val first: String, val last: String)

/**
 * When a name day falls: a fixed church (Julian) date, a distance from Pascha,
 * or a weekday inside a window of church dates (`scripts/shared/fixed_cycle.py`'s
 * `between` rule in the iOS repo). Stored this way, never as a civil date, so it
 * lands on the right day every year — like [SlavaAnchor], whose arithmetic it
 * reuses.
 *
 * Mirror of `NameDayAnchor` in `OrthodoxCalendar/Models/NameDay.swift`.
 */
@Serializable
sealed interface NameDayAnchor {
    @Serializable @SerialName("julian")
    data class Julian(val month: Int, val day: Int) : NameDayAnchor

    @Serializable @SerialName("pascha")
    data class Pascha(val offset: Int) : NameDayAnchor

    @Serializable @SerialName("weekday")
    data class Weekday(val windows: List<NameDayWindow>) : NameDayAnchor

    val isMoveable: Boolean get() = this !is Julian

    /**
     * Whether [day] is this commemoration, read from the church date, weekday
     * and Pascha distance the data carries — so it agrees with the calendar.
     */
    fun matches(day: CalendarDay): Boolean {
        slavaForm(this)?.let { return it.matches(day) }
        val windows = (this as? Weekday)?.windows ?: return false
        return windows.any { w -> day.dayOfWeek == w.weekday && inside(day.julianDate, w) }
    }

    /**
     * The date in church year [year] (Pascha's year, or the Julian year of a
     * fixed date: Julian 29 December 2026 is 11 January 2027).
     */
    fun occurrence(year: Int): LocalDate? {
        slavaForm(this)?.let { return it.occurrence(year) }
        val windows = (this as? Weekday)?.windows ?: return null
        var best: LocalDate? = null
        for (w in windows) {
            val (fm, fd) = monthDay(w.first)
            val (lm, ld) = monthDay(w.last)
            val start = runCatching { ChurchDates.gregorian(year, fm, fd) }.getOrNull() ?: continue
            val end = runCatching {
                ChurchDates.gregorian(if (w.first <= w.last) year else year + 1, lm, ld)
            }.getOrNull() ?: continue
            var d = start
            while (!d.isAfter(end)) {
                // java.time: Monday = 1 … Sunday = 7; the data: Monday = 0.
                if (d.dayOfWeek.value - 1 == w.weekday) {
                    if (best == null || d.isBefore(best)) best = d
                    break
                }
                d = d.plusDays(1)
            }
        }
        return best
    }

    /** The first day this falls on, on or after [start]. */
    fun nextOccurrence(start: LocalDate): LocalDate? {
        for (y in (start.year - 1)..(start.year + 1)) {
            val date = occurrence(y) ?: continue
            if (!date.isBefore(start)) return date
        }
        return null
    }

    companion object {
        /** The slava form of a fixed or Pascha-bound anchor: the same date rules. */
        private fun slavaForm(anchor: NameDayAnchor): SlavaDay? = when (anchor) {
            is Julian -> SlavaDay("", "", SlavaAnchor.Julian(anchor.month, anchor.day))
            is Pascha -> SlavaDay("", "", SlavaAnchor.Pascha(anchor.offset))
            is Weekday -> null
        }

        private fun inside(key: String, w: NameDayWindow): Boolean =
            if (w.first <= w.last) w.first <= key && key <= w.last
            else key >= w.first || key <= w.last

        fun monthDay(key: String): Pair<Int, Int> {
            val parts = key.split("-").mapNotNull { it.toIntOrNull() }
            return if (parts.size == 2) parts[0] to parts[1] else 1 to 1
        }
    }
}

/**
 * Whose name day it is and which saint's: the name as the person uses it
 * ("Таня"), the church form it maps to ("Татиана"), and the commemoration
 * chosen — found from the birthday or picked by hand.
 */
@Serializable
data class NameDayChoice(
    val name: String,
    val churchName: String,
    val title: String,
    val anchor: NameDayAnchor,
    /** Civil birthday month and day, when the name day was found from it. */
    val birthMonth: Int? = null,
    val birthDay: Int? = null
) {
    fun matches(day: CalendarDay): Boolean = anchor.matches(day)
    fun nextOccurrence(start: LocalDate): LocalDate? = anchor.nextOccurrence(start)
}

/** A friend's or relative's name day. */
@Serializable
data class FriendNameDay(
    val id: String = UUID.randomUUID().toString(),
    /** How the user calls them: "мама", "кум Сергей"; the name when empty. */
    val person: String,
    val nameDay: NameDayChoice
) {
    val displayName: String get() = person.ifEmpty { nameDay.name }
}

/** What a calendar row shows about name days on its day. */
data class NameDayMark(
    val isMine: Boolean = false,
    /** "мама · Галина", one per friend whose name day falls here. */
    val friendLines: List<String> = emptyList()
) {
    val isEmpty: Boolean get() = !isMine && friendLines.isEmpty()
}

/** One name in a day's "Именины" list. */
data class DayName(
    val name: String,
    /** A main saint of the day (the source's first paragraph). */
    val isMain: Boolean
)

/** The user's next name day as the countdown sees it. */
data class NameDayCountdown(val nameDay: NameDayChoice, val date: LocalDate, val days: Int)

/**
 * The user's name day, friends' name days, and how to be reminded. Persisted as
 * one JSON value by [NameDayStore].
 */
@Serializable
data class NameDaySettings(
    val mine: NameDayChoice? = null,
    val friends: List<FriendNameDay> = emptyList(),
    val remindOnDay: Boolean = true,
    val remindFriends: Boolean = true,
    val remindFriendsDayBefore: Boolean = false,
    /** Let new martyrs decide an automatic name day (azbyka.ru leaves them out
     *  unless asked). A saint chosen by hand is never restricted. */
    val includeNewMartyrs: Boolean = false,
    /** Minutes after midnight the reminders fire at. */
    val reminderMinutes: Int = 9 * 60
) {
    /** What a calendar row shows for [day], or null when no name day falls on it. */
    fun mark(day: CalendarDay): NameDayMark? {
        val isMine = mine?.matches(day) == true
        val lines = friends.filter { it.nameDay.matches(day) }
            .map { "${it.displayName} · ${it.nameDay.churchName}" }
        return NameDayMark(isMine, lines).takeUnless { it.isEmpty }
    }

    /** The user's next name day and how many days away it is (0 = today). */
    fun countdown(today: LocalDate): NameDayCountdown? {
        val choice = mine ?: return null
        val date = choice.nextOccurrence(today) ?: return null
        return NameDayCountdown(choice, date, ChronoUnit.DAYS.between(today, date).toInt())
    }
}

/** Russian wording for name days, shared by the screens and the reminders. */
object NameDayText {
    /** "через 5 дней", with Russian plurals: 1 день, 2–4 дня, 5–20 дней, 21 день. */
    fun countdown(days: Int): String = when (days) {
        0 -> "С днём ангела!"
        1 -> "завтра"
        else -> "через $days ${daysWord(days)}"
    }

    fun daysWord(n: Int): String {
        val mod10 = n % 10
        val mod100 = n % 100
        if (mod10 == 1 && mod100 != 11) return "день"
        if (mod10 in 2..4 && mod100 !in 12..14) return "дня"
        return "дней"
    }

    val monthsGenitive = listOf(
        "января", "февраля", "марта", "апреля", "мая", "июня",
        "июля", "августа", "сентября", "октября", "ноября", "декабря"
    )

    val monthsNominative = listOf(
        "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
        "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"
    )

    /** Sunday first. */
    val weekdays = listOf("воскресенье", "понедельник", "вторник", "среда", "четверг", "пятница", "суббота")

    /** "четверг, 25 января". */
    fun longDate(date: LocalDate): String =
        "${weekdays[date.dayOfWeek.value % 7]}, ${dayAndMonth(date)}"

    /** "25 января". */
    fun dayAndMonth(date: LocalDate): String = "${date.dayOfMonth} ${monthsGenitive[date.monthValue - 1]}"

    /**
     * "25 января (12 января по ст. ст.)", or for one that moves
     * "переходящая, ближайшая 26 апреля". Mirror of iOS `NameDayLabel.when`.
     */
    fun whenText(anchor: NameDayAnchor, today: LocalDate = LocalDate.now()): String {
        val civil = anchor.nextOccurrence(today)?.let(::dayAndMonth).orEmpty()
        return when (anchor) {
            is NameDayAnchor.Julian -> "$civil (${anchor.day} ${monthsGenitive[(anchor.month - 1) % 12]} по ст. ст.)"
            else -> "переходящая, ближайшая $civil"
        }
    }

    /** Days in a birthday month (February has 29: a birthday, not a year). */
    fun daysIn(month: Int): Int = intArrayOf(31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)[(month - 1) % 12]
}

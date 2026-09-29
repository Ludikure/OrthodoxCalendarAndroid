package com.orthodox.calendar.data.slava

import com.orthodox.calendar.data.model.CalendarDay
import com.orthodox.calendar.engine.ChurchDates
import com.orthodox.calendar.engine.Paschalion
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Locale
import java.util.UUID

/**
 * When a slava falls: on a fixed church (Julian) date, or a set number of days
 * from Pascha. Stored this way — never as a civil date — so it lands on the
 * right day in every year, bundled or downloaded, without touching the data.
 *
 * Mirror of `SlavaAnchor` in `OrthodoxCalendar/Models/Slava.swift`.
 */
@Serializable
sealed interface SlavaAnchor {
    @Serializable @SerialName("julian")
    data class Julian(val month: Int, val day: Int) : SlavaAnchor

    @Serializable @SerialName("pascha")
    data class Pascha(val offset: Int) : SlavaAnchor
}

/**
 * A slava a family keeps: what people call it ("Никољдан"), the saint or feast
 * it honours, and when it falls.
 */
@Serializable
data class SlavaDay(
    val name: String,
    val saint: String,
    val anchor: SlavaAnchor
) {
    val id: String
        get() = when (val a = anchor) {
            is SlavaAnchor.Julian -> String.format(Locale.ROOT, "j%02d-%02d:", a.month, a.day) + name
            is SlavaAnchor.Pascha -> "p${a.offset}:" + name
        }

    val isMoveable: Boolean get() = anchor is SlavaAnchor.Pascha

    /**
     * Whether [day] is this slava. Reads the church date and Pascha distance the
     * data already carries, so it agrees with the calendar on screen.
     */
    fun matches(day: CalendarDay): Boolean = when (val a = anchor) {
        is SlavaAnchor.Pascha -> day.paschaDistance == a.offset
        is SlavaAnchor.Julian -> {
            val key = String.format(Locale.ROOT, "%02d-%02d", a.month, a.day)
            if (day.julianDate == key) {
                true
            } else {
                // Julian February 29 only exists every fourth year; in the others
                // its commemorations are kept on the 28th.
                val year = day.gregorianDate.take(4).toIntOrNull()
                a.month == 2 && a.day == 29 && day.julianDate == "02-28" &&
                    year != null && !ChurchDates.isJulianLeap(year)
            }
        }
    }

    /** The first day this slava falls on, on or after [start]. */
    fun nextOccurrence(start: LocalDate): LocalDate? {
        for (y in (start.year - 1)..(start.year + 1)) {
            val date = occurrence(y) ?: continue
            if (!date.isBefore(start)) return date
        }
        return null
    }

    /**
     * This slava's date in church year [year] (Pascha's year, or the Julian year
     * of a fixed date — Christmas of Julian 2026 is 7 January 2027).
     */
    fun occurrence(year: Int): LocalDate? = runCatching {
        when (val a = anchor) {
            is SlavaAnchor.Pascha -> Paschalion.pascha(year).plusDays(a.offset.toLong())
            is SlavaAnchor.Julian -> ChurchDates.gregorian(year, a.month, a.day)
        }
    }.getOrNull()
}

/** Another family's slava, so the user doesn't miss the ones they visit. */
@Serializable
data class FriendSlava(
    val id: String = UUID.randomUUID().toString(),
    val person: String,
    val slava: SlavaDay
)

/** What a calendar row shows about slavas on its day. */
data class SlavaMark(
    /** The user's own slava falls here. */
    val isMine: Boolean = false,
    /** "Андрејевдан · Петровићи", one per friend whose slava falls here. */
    val friendLines: List<String> = emptyList()
) {
    val isEmpty: Boolean get() = !isMine && friendLines.isEmpty()
}

/**
 * The user's next slava — or, in Russian, name day (`NameDayCountdown`) — as
 * the banner shows it. Mirror of iOS `SlavaCountdown`, which carries the same
 * [kind].
 */
data class SlavaCountdown(
    val name: String,
    val date: LocalDate,
    /** 0 = today. */
    val days: Int,
    val kind: Kind = Kind.SLAVA
) {
    enum class Kind { SLAVA, NAME_DAY }
}

/**
 * The user's slava, friends' slavas, and how to be reminded. Persisted as one
 * JSON value by [SlavaStore].
 */
@Serializable
data class SlavaSettings(
    val mine: SlavaDay? = null,
    val friends: List<FriendSlava> = emptyList(),
    val remindWeekBefore: Boolean = true,
    val remindOnDay: Boolean = true,
    val remindFriends: Boolean = true,
    /** Minutes after midnight the reminders fire at. */
    val reminderMinutes: Int = 9 * 60
) {
    /** What a calendar row shows for [day], or null when no slava falls on it. */
    fun mark(day: CalendarDay): SlavaMark? {
        val isMine = mine?.matches(day) == true
        val lines = friends.filter { it.slava.matches(day) }.map { "${it.slava.name} · ${it.person}" }
        return SlavaMark(isMine, lines).takeUnless { it.isEmpty }
    }

    /** The user's next slava and how many days away it is (0 = today). */
    fun countdown(today: LocalDate): SlavaCountdown? {
        val slava = mine ?: return null
        val date = slava.nextOccurrence(today) ?: return null
        return SlavaCountdown(slava.name, date, ChronoUnit.DAYS.between(today, date).toInt())
    }
}

/** Serbian wording shared by the slava screens and the reminders. */
object SlavaText {
    val months = listOf(
        "јануар", "фебруар", "март", "април", "мај", "јун",
        "јул", "август", "септембар", "октобар", "новембар", "децембар"
    )

    /** Sunday first, like iOS `Calendar.component(.weekday)` minus one. */
    val weekdays = listOf("недеља", "понедељак", "уторак", "среда", "четвртак", "петак", "субота")

    /** What the slava table may hold, from the day's fasting level. */
    fun table(fastingType: String): String = when (fastingType) {
        "free" -> "Трпеза је мрсна."
        "fish", "fishRoe" -> "Трпеза је посна — риба је дозвољена."
        "hotWithOil" -> "Трпеза је посна — на уљу, без рибе."
        "hotNoOil" -> "Трпеза је посна — на води, без уља."
        else -> "Трпеза је посна — строги пост."
    }

    /**
     * "за 12 дана", with Serbian's plural forms: 1 дан, 2–4 дана, 5+ дана
     * (21 дан, 22 дана, but 11–14 дана). Mirror of `SeasonBanner.countdownLabel`.
     */
    fun countdownLabel(days: Int): String = when (days) {
        0 -> "Срећна слава!"
        1 -> "сутра"
        else -> {
            val word = if (days % 10 == 1 && days % 100 != 11) "дан" else "дана"
            "за $days $word"
        }
    }

    /** "субота 19. децембар", as the friend reminder words a date. */
    fun longDate(date: LocalDate): String {
        // java.time: Monday = 1 … Sunday = 7; the table is Sunday first.
        val weekday = weekdays[date.dayOfWeek.value % 7]
        return "$weekday ${date.dayOfMonth}. ${months[date.monthValue - 1]}"
    }
}

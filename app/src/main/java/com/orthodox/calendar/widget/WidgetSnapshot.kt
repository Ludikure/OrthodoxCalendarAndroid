package com.orthodox.calendar.widget

import android.content.Context
import com.orthodox.calendar.data.model.AppLanguage
import com.orthodox.calendar.data.model.CalendarDay
import com.orthodox.calendar.data.model.UILabels
import com.orthodox.calendar.data.slava.SlavaDay
import com.orthodox.calendar.data.slava.SlavaStore
import com.orthodox.calendar.ui.util.toIsoDate
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * What the home-screen widgets show, written by the app to its private files
 * and read by the widget.
 *
 * The widget never opens a year file: a resolved year pulls in a text pool of
 * up to 17 MB, and a widget update can run in a cold process with nothing else
 * loaded. The app, which has the year loaded anyway, writes the next few weeks
 * here — a few kilobytes of already-localized strings — and the widget only
 * picks the entry for the day it is drawing.
 *
 * Same fields and JSON shape as `Shared/WidgetSnapshot.swift` in the iOS repo.
 */
@Serializable
data class WidgetSnapshot(
    val version: Int = CURRENT_VERSION,
    /** [AppLanguage.code]: sr, ru, en, en_nc. */
    val language: String,
    /** ISO-8601 instant. */
    val generatedAt: String,
    /** Consecutive days, the first one the day the snapshot was written. */
    val days: List<Day>
) {
    @Serializable
    data class Day(
        /** `yyyy-MM-dd`. */
        val date: String,
        /** "Недеља", "Sunday". */
        val weekday: String,
        /** "27 Септембар", "27 сентября". */
        val dateLabel: String,
        val primary: String,
        /** Secondary and tertiary commemorations, a few, each kept short. */
        val secondary: List<String>,
        val isGreatFeast: Boolean,
        val fastingType: String,
        val fastingLabel: String,
        val fastingAbbrev: String,
        /** The user's slava within the countdown window, counted from this day
         *  (0 = this day is the slava). Serbian only. */
        val slava: Slava? = null
    )

    @Serializable
    data class Slava(val name: String, val daysUntil: Int)

    /**
     * The entry for [key] (`yyyy-MM-dd`), or null when the snapshot does not
     * reach that day — the widget then asks the user to open the app instead
     * of showing another day's saints.
     */
    fun day(key: String): Day? = days.firstOrNull { it.date == key }

    companion object {
        const val FILE_NAME = "widget_snapshot.json"
        /** How many days the app writes ahead. */
        const val DAY_COUNT = 21
        /** Bumped when the format changes incompatibly; an older file is ignored. */
        const val CURRENT_VERSION = 1

        private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

        fun file(context: Context): File = File(context.filesDir, FILE_NAME)

        fun encode(snapshot: WidgetSnapshot): String = json.encodeToString(serializer(), snapshot)

        /** Null for a missing, unreadable or older-format snapshot. */
        fun decode(text: String): WidgetSnapshot? =
            runCatching { json.decodeFromString(serializer(), text) }.getOrNull()
                ?.takeIf { it.version == CURRENT_VERSION }

        fun read(file: File): WidgetSnapshot? =
            runCatching { file.readText() }.getOrNull()?.let(::decode)

        fun read(context: Context): WidgetSnapshot? = read(file(context))

        /** Written to a temporary file and renamed, so a widget never reads half of one. */
        fun write(snapshot: WidgetSnapshot, file: File) {
            val tmp = File(file.parentFile, file.name + ".tmp")
            tmp.writeText(encode(snapshot))
            if (!tmp.renameTo(file)) {
                file.writeText(encode(snapshot))
                tmp.delete()
            }
        }
    }
}

/**
 * Builds the widgets' snapshot from loaded calendar days. Pure, so the tests
 * run it against a bundled year. Mirror of `WidgetSnapshotBuilder` in
 * `OrthodoxCalendar/App/WidgetSync.swift`.
 */
object WidgetSnapshotBuilder {
    /** How many days before the slava the countdown shows: the month banner's window. */
    const val SLAVA_WINDOW = SlavaStore.BANNER_DAYS
    /** Secondary commemorations kept per day, and the length each is cut to. */
    const val MAX_SECONDARY = 3
    const val MAX_SECONDARY_LENGTH = 90

    /**
     * [WidgetSnapshot.DAY_COUNT] consecutive days starting on [start], taken
     * from [days] (keyed by `gregorianDate`). Stops at the first day [days]
     * does not have; null if it does not have [start] itself.
     */
    fun build(
        days: Map<String, CalendarDay>,
        start: LocalDate,
        language: AppLanguage,
        ui: UILabels,
        mySlava: SlavaDay?,
        now: Instant = Instant.now()
    ): WidgetSnapshot? {
        val out = mutableListOf<WidgetSnapshot.Day>()
        for (offset in 0 until WidgetSnapshot.DAY_COUNT) {
            val date = start.plusDays(offset.toLong())
            val day = days[date.toIsoDate()] ?: break
            out += entry(day, date, language, ui, mySlava)
        }
        if (out.isEmpty()) return null
        return WidgetSnapshot(language = language.code, generatedAt = now.toString(), days = out)
    }

    private fun entry(
        day: CalendarDay, date: LocalDate, language: AppLanguage, ui: UILabels, mySlava: SlavaDay?
    ): WidgetSnapshot.Day {
        val primary = day.primaryFeast?.name ?: day.feasts.firstOrNull()?.name ?: ""
        val secondary = (day.secondaryFeasts + day.tertiaryFeasts)
            .map { it.name }
            .filter { it.isNotEmpty() && it != primary }
            .take(MAX_SECONDARY)
            .map(::shorten)
        return WidgetSnapshot.Day(
            date = day.gregorianDate,
            weekday = ui.daysOfWeekFull.getOrElse(day.weekdayIndex) { "" },
            dateLabel = ui.dayAndMonth(day.gregorianDay, day.gregorianMonth),
            primary = primary,
            secondary = secondary,
            isGreatFeast = day.isGreatFeast,
            fastingType = day.fasting.type,
            fastingLabel = day.fasting.label,
            fastingAbbrev = day.fasting.abbrev.orEmpty(),
            slava = slava(date, language, mySlava)
        )
    }

    /** The user's slava counted from [date], within the banner's window. Serbian only. */
    private fun slava(date: LocalDate, language: AppLanguage, mine: SlavaDay?): WidgetSnapshot.Slava? {
        if (language != AppLanguage.SR || mine == null) return null
        val next = mine.nextOccurrence(date) ?: return null
        val days = ChronoUnit.DAYS.between(date, next).toInt()
        if (days > SLAVA_WINDOW) return null
        return WidgetSnapshot.Slava(mine.name, days)
    }

    internal fun shorten(name: String): String {
        if (name.length <= MAX_SECONDARY_LENGTH) return name
        return name.take(MAX_SECONDARY_LENGTH - 1).trim() + "…"
    }
}

package com.orthodox.calendar.data.model

import java.time.LocalDate

/**
 * A day's place within a named fasting season (Great Lent, Nativity Fast, etc.).
 * [dayIndex] is 1-based; [total] is the length of the contiguous run.
 */
data class FastingPeriodInfo(
    val code: String,
    val displayName: String,
    val start: LocalDate,
    val end: LocalDate,
    val dayIndex: Int,
    val total: Int,
    /**
     * False when the run touches the edge of the available data, so its true
     * start/end (and therefore the index/total) may extend beyond what we loaded
     * — e.g. the Nativity Fast spanning Dec into the next year's file. Callers
     * should then show the season name only, not a misleading "Day X of Y".
     */
    val complete: Boolean
)

/**
 * Resolves the named fasting seasons carried by [CalendarDay.fastingPeriod].
 *
 * The data tags days with snake_case codes (`great_lent`, ...) while the
 * localization bundle keys its display names by the canonical English label,
 * so we bridge the two here. Mirrors the iOS data format; iOS does not yet
 * surface this either.
 */
object FastingPeriods {

    /** Data code -> canonical key used in `LocalizationBundle.fastingPeriodNames`. */
    private val CODE_TO_KEY = mapOf(
        "great_lent" to "Great Lent",
        "apostles_fast" to "Apostles' Fast",
        "dormition_fast" to "Dormition Fast",
        "nativity_fast" to "Nativity Fast"
    )

    /** Localized "Day X of Y" label. */
    fun dayLabel(language: AppLanguage, index: Int, total: Int): String = when (language) {
        AppLanguage.SR -> "Дан $index од $total"
        AppLanguage.RU -> "День $index из $total"
        AppLanguage.EN, AppLanguage.EN_NC -> "Day $index of $total"
    }

    /** Compact "23 Feb – 11 Apr" range, using the localized month names. */
    fun dateRange(info: FastingPeriodInfo, months: List<String>): String {
        fun fmt(d: LocalDate) = "${d.dayOfMonth} ${months.getOrElse(d.monthValue - 1) { "" }.take(3)}"
        return "${fmt(info.start)} – ${fmt(info.end)}"
    }

    fun displayName(code: String, names: Map<String, String>): String {
        val key = CODE_TO_KEY[code]
            ?: code.split('_').joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
        return names[key] ?: key
    }

    /**
     * Maps each in-season day (by `gregorianDate`) to the contiguous run it sits
     * in. [days] should be the widest range available (a full year), in any order;
     * runs are broken by a gap in dates or a change of period code.
     */
    fun computeSpans(
        days: List<CalendarDay>,
        names: Map<String, String>
    ): Map<String, FastingPeriodInfo> {
        // The available date window; a run reaching either edge may continue
        // beyond the loaded data (e.g. Nativity Fast crossing the year boundary).
        val allDates = days.map { it.gregorianDate }
        val minDate = allDates.minOrNull()
        val maxDate = allDates.maxOrNull()

        val sorted = days.filter { it.fastingPeriod != null }.sortedBy { it.gregorianDate }
        val result = HashMap<String, FastingPeriodInfo>()
        var i = 0
        while (i < sorted.size) {
            val code = sorted[i].fastingPeriod!!
            var j = i
            while (j + 1 < sorted.size) {
                val next = sorted[j + 1]
                val consecutive = LocalDate.parse(next.gregorianDate) ==
                    LocalDate.parse(sorted[j].gregorianDate).plusDays(1)
                if (next.fastingPeriod == code && consecutive) j++ else break
            }
            val run = sorted.subList(i, j + 1)
            val start = LocalDate.parse(run.first().gregorianDate)
            val end = LocalDate.parse(run.last().gregorianDate)
            val complete = run.first().gregorianDate != minDate && run.last().gregorianDate != maxDate
            val name = displayName(code, names)
            run.forEachIndexed { idx, d ->
                result[d.gregorianDate] =
                    FastingPeriodInfo(code, name, start, end, idx + 1, run.size, complete)
            }
            i = j + 1
        }
        return result
    }
}

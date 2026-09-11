package com.orthodox.calendar.ui.util

import java.util.Locale
import com.orthodox.calendar.data.model.AppLanguage
import com.orthodox.calendar.data.model.ScriptureReading

/**
 * The service label on a reading card.
 *
 * The pipeline writes two candidates into the JSON. `service` is already in the
 * calendar's language, but it is only set on menaion readings in the Serbian
 * data — 77 of ~2000 a year, and none at all in Russian or English — so the
 * card's service slot was blank for over 96% of readings. `source` is set on
 * essentially every reading ("Vespers", "8th Matins Gospel", "3rd Hour,
 * Prophecy") but comes from the lectionary tables and is therefore always
 * English, which is why it is translated here instead of shown raw.
 *
 * The vocabulary is closed: an ordinal plus one of a dozen nouns, occasionally
 * two comma-separated parts. An unrecognised value yields no label rather than
 * English text in a Serbian or Russian UI.
 *
 * These strings live here rather than in the localization bundles, which are
 * copied byte-for-byte from the iOS repo and must stay identical to it.
 */
fun serviceLabel(reading: ScriptureReading, language: AppLanguage): String? {
    reading.service?.takeIf { it.isNotBlank() }?.let { return it }
    val source = reading.source?.takeIf { it.isNotBlank() } ?: return null
    val parts = source.split(',').mapNotNull { sourcePart(it.trim(), language) }
    if (parts.isEmpty()) return null
    return parts.joinToString(", ").replaceFirstChar { it.uppercase(Locale.ROOT) }
}

private val ORDINAL = Regex("""^(\d{1,2})(?:st|nd|rd|th)\s+(.+)$""")

private fun sourcePart(part: String, language: AppLanguage): String? {
    val match = ORDINAL.matchEntire(part)
    val ordinal = match?.groupValues?.get(1)?.toIntOrNull()
    val noun = (match?.groupValues?.get(2) ?: part).trim().lowercase(Locale.ROOT)

    return when (noun) {
        // "Gospel"/"Epistle"/"Prophecy" only restate the reading's own type,
        // which the card prints immediately beside this label, and "fixed" is
        // the pipeline's marker for a menaion reading, not a service.
        "gospel", "epistle", "prophecy", "fixed" -> null

        "vespers", "vespers gospel" -> when (language) {
            AppLanguage.SR -> "Вечерња"
            AppLanguage.RU -> "Вечерня"
            AppLanguage.EN, AppLanguage.EN_NC -> "Vespers"
        }

        "matins", "matins epistle" -> matins(language)

        // Without an ordinal this is simply the Matins gospel; with one it is
        // the numbered resurrectional gospel of the eleven-week cycle.
        "matins gospel" ->
            if (ordinal == null) matins(language) else when (language) {
                AppLanguage.SR -> "$ordinal. јутрењско јеванђеље"
                AppLanguage.RU -> "$ordinal-е утреннее Евангелие"
                AppLanguage.EN, AppLanguage.EN_NC -> "${ordinalEn(ordinal)} Matins Gospel"
            }

        "passion gospel" -> when (language) {
            AppLanguage.SR ->
                if (ordinal == null) "страсно јеванђеље" else "$ordinal. страсно јеванђеље"
            AppLanguage.RU ->
                if (ordinal == null) "Страстное Евангелие" else "$ordinal-е Страстное Евангелие"
            AppLanguage.EN, AppLanguage.EN_NC ->
                if (ordinal == null) "Passion Gospel" else "${ordinalEn(ordinal)} Passion Gospel"
        }

        // "Sixth Hour" occurs alongside "6th Hour" in the tables.
        "hour", "sixth hour" -> hour(ordinal ?: 6.takeIf { noun == "sixth hour" }, language)

        "cross procession" -> when (language) {
            AppLanguage.SR -> "Изношење Часног крста"
            AppLanguage.RU -> "Изнесение Честного Креста"
            AppLanguage.EN, AppLanguage.EN_NC -> "Cross Procession"
        }

        "great blessing of waters" -> when (language) {
            AppLanguage.SR -> "Велико освећење воде"
            AppLanguage.RU -> "Великое освящение воды"
            AppLanguage.EN, AppLanguage.EN_NC -> "Great Blessing of Waters"
        }

        else -> null
    }
}

private fun matins(language: AppLanguage): String = when (language) {
    AppLanguage.SR -> "Јутрења"
    AppLanguage.RU -> "Утреня"
    AppLanguage.EN, AppLanguage.EN_NC -> "Matins"
}

private fun hour(ordinal: Int?, language: AppLanguage): String? {
    if (ordinal == null) return null
    return when (language) {
        AppLanguage.SR -> "$ordinal. час"
        AppLanguage.RU -> "$ordinal-й час"
        AppLanguage.EN, AppLanguage.EN_NC -> "${ordinalEn(ordinal)} Hour"
    }
}

private fun ordinalEn(n: Int): String {
    val suffix = when {
        n % 100 in 11..13 -> "th"
        n % 10 == 1 -> "st"
        n % 10 == 2 -> "nd"
        n % 10 == 3 -> "rd"
        else -> "th"
    }
    return "$n$suffix"
}

/**
 * The reading's own type, localized.
 *
 * Returns null for anything outside the three known types. The data carries a
 * handful of `other` readings a year — the odes of the Great Canon, whose
 * `title` ("Песма прва") already says what they are — and the card used to
 * print the literal word OTHER above them.
 */
fun readingTypeLabel(type: String, language: AppLanguage): String? =
    when (type.lowercase(Locale.ROOT)) {
    "gospel" -> when (language) {
        AppLanguage.SR -> "ЈЕВАНЂЕЉЕ"
        AppLanguage.RU -> "ЕВАНГЕЛИЕ"
        AppLanguage.EN, AppLanguage.EN_NC -> "GOSPEL"
    }
    "apostol" -> when (language) {
        AppLanguage.SR, AppLanguage.RU -> "АПОСТОЛ"
        AppLanguage.EN, AppLanguage.EN_NC -> "EPISTLE"
    }
    "ot" -> when (language) {
        AppLanguage.SR -> "СТАРИ ЗАВЕТ"
        AppLanguage.RU -> "ВЕТХИЙ ЗАВЕТ"
        AppLanguage.EN, AppLanguage.EN_NC -> "OLD TESTAMENT"
    }
    else -> null
}

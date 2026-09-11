package com.orthodox.calendar.data.model

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

/**
 * A date after a day number reads as the language writes it: Russian declines
 * the month ("19 декабря", not the header form "19 Декабрь"); the other
 * languages use their month names as they are. Checked against the bundled files.
 */
class DayAndMonthTest {
    private val json = Json { ignoreUnknownKeys = true }

    private fun labels(lang: String): UILabels {
        for (path in listOf("src/main/assets/localization", "app/src/main/assets/localization")) {
            val file = File(path, "$lang.json")
            if (file.exists()) return json.decodeFromString(LocalizationBundle.serializer(), file.readText()).ui
        }
        throw IllegalStateException("$lang.json not found from ${File(".").absolutePath}")
    }

    @Test
    fun russianTakesTheGenitive() {
        val ru = labels("ru")
        assertEquals("19 декабря", ru.dayAndMonth(19, 12))
        assertEquals("2 мая", ru.dayAndMonth(2, 5))
        assertEquals(12, ru.monthsGenitive?.size)
    }

    @Test
    fun otherLanguagesKeepTheirMonthNames() {
        assertEquals("19 December", labels("en").dayAndMonth(19, 12))
        assertEquals("19 Децембар", labels("sr").dayAndMonth(19, 12))
    }

    @Test
    fun anOutOfRangeMonthLeavesTheDay() {
        assertEquals("7", labels("ru").dayAndMonth(7, 13))
    }
}

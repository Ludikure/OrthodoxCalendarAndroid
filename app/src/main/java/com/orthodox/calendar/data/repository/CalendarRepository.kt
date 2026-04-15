package com.orthodox.calendar.data.repository

import android.content.Context
import com.orthodox.calendar.data.model.CalendarDay
import com.orthodox.calendar.data.model.CalendarFile
import com.orthodox.calendar.data.model.LocalizationBundle
import kotlinx.serialization.json.Json

class CalendarRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }
    private val cache = mutableMapOf<String, CalendarFile>()

    fun loadMonth(locale: String, year: Int, month: Int): List<CalendarDay> {
        val file = loadCalendarFile(locale, year) ?: return emptyList()
        val prefix = "%02d-".format(month)
        return file.days
            .filter { it.key.startsWith(prefix) }
            .entries
            .sortedBy { it.key }
            .map { it.value }
    }

    fun loadCalendarFile(locale: String, year: Int): CalendarFile? {
        val key = "${locale}_${year}"
        cache[key]?.let { return it }

        val filename = "localization/calendar_${locale}_${year}.json"
        return try {
            val jsonString = context.assets.open(filename).bufferedReader().use { it.readText() }
            val file = json.decodeFromString<CalendarFile>(jsonString)
            cache[key] = file
            file
        } catch (e: Exception) {
            null
        }
    }

    fun loadLocalizationBundle(localeFile: String): LocalizationBundle? {
        val filename = "localization/${localeFile}.json"
        return try {
            val jsonString = context.assets.open(filename).bufferedReader().use { it.readText() }
            json.decodeFromString<LocalizationBundle>(jsonString)
        } catch (e: Exception) {
            null
        }
    }
}

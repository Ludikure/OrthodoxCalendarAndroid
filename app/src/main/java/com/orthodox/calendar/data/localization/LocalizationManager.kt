package com.orthodox.calendar.data.localization

import android.content.Context
import com.orthodox.calendar.data.model.AppLanguage
import com.orthodox.calendar.data.model.LocalizationBundle
import com.orthodox.calendar.data.model.UILabels
import kotlinx.serialization.json.Json

class LocalizationManager(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }
    private val cache = mutableMapOf<String, LocalizationBundle>()

    fun loadBundle(language: AppLanguage): LocalizationBundle {
        val locFile = language.localizationFile
        cache[locFile]?.let { return it }

        val filename = "localization/${locFile}.json"
        return try {
            val jsonString = context.assets.open(filename).bufferedReader().use { it.readText() }
            val bundle = json.decodeFromString<LocalizationBundle>(jsonString)
            cache[locFile] = bundle
            bundle
        } catch (e: Exception) {
            // Fallback to Serbian
            if (language != AppLanguage.SR) {
                return loadBundle(AppLanguage.SR)
            }
            // Emergency fallback with minimal data
            LocalizationBundle(
                language = "sr",
                displayName = "Srpski",
                script = "cyrillic",
                ui = UILabels(
                    appTitle = "Orthodox Calendar",
                    months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"),
                    daysOfWeek = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"),
                    daysOfWeekFull = listOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"),
                    julianLabel = "Julian",
                    fastingLabel = "Fasting",
                    readingsLabel = "Readings",
                    commemorationsLabel = "Commemorations",
                    settingsLabel = "Settings",
                    todayLabel = "Today"
                )
            )
        }
    }

    fun localizedMonthName(bundle: LocalizationBundle, month: Int): String {
        if (month < 1 || month > 12) return ""
        return bundle.ui.months[month - 1]
    }

    fun localizedDayOfWeek(bundle: LocalizationBundle, weekday: Int): String {
        if (weekday < 0 || weekday > 6) return ""
        return bundle.ui.daysOfWeek[weekday]
    }
}

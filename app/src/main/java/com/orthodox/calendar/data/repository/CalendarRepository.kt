package com.orthodox.calendar.data.repository

import android.content.Context
import com.orthodox.calendar.data.model.CalendarDay
import com.orthodox.calendar.data.model.CalendarFile
import com.orthodox.calendar.data.model.LocalizationBundle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream

/**
 * Loads a year of calendar data for a given locale from the app bundle.
 *
 * All supported years (MIN_YEAR..MAX_YEAR) ship bundled and deduplicated: large
 * text (saint bios + scripture readings) lives in a per-locale `texts_<locale>`
 * pool keyed by content hash, and the calendar files reference it. There is no
 * network path — the app is fully offline. Mirror of iOS `CalendarRepository`.
 */
@OptIn(ExperimentalSerializationApi::class)
class CalendarRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }
    private val cache = mutableMapOf<String, CalendarFile>()
    /** Per-locale deduped text pool (texts_<locale>.json), loaded lazily. */
    private val textsCache = mutableMapOf<String, Map<String, String>>()

    sealed class LoadError : Exception() {
        /** No bundled data for this locale/year. */
        object NotFound : LoadError()
    }

    private fun fileKey(locale: String, year: Int) = "calendar_${locale}_${year}"

    /** Days for a single month; throws [LoadError] when the year isn't bundled. */
    suspend fun loadMonth(locale: String, year: Int, month: Int): List<CalendarDay> {
        val file = load(locale, year)
        val prefix = "%02d-".format(month)
        return file.days
            .filter { it.key.startsWith(prefix) }
            .entries
            .sortedBy { it.key }
            .map { it.value }
    }

    /** Resolve a year from the bundle (decoded once, cached in memory). */
    suspend fun load(locale: String, year: Int): CalendarFile {
        val key = fileKey(locale, year)
        cache[key]?.let { return it }
        val file = withContext(Dispatchers.IO) {
            decodeAsset(key)?.let { resolveText(it, locale) } ?: throw LoadError.NotFound
        }
        cache[key] = file
        return file
    }

    /** Fills bio + reading text from the per-locale pool for deduped bundled data. */
    private fun resolveText(file: CalendarFile, locale: String): CalendarFile {
        val needs = file.days.values.any { d ->
            d.saintBios?.any { it.ref != null } == true ||
                d.readings.any { it.textRef != null || it.textWebRef != null }
        }
        if (!needs) return file
        val pool = textsPool(locale)
        val days = file.days.mapValues { (_, day) ->
            day.copy(
                saintBios = day.saintBios?.map { b ->
                    if (b.ref != null && b.text.isEmpty()) b.copy(text = pool[b.ref] ?: "") else b
                },
                readings = day.readings.map { r ->
                    var out = r
                    if (r.textRef != null && r.text == null) out = out.copy(text = pool[r.textRef])
                    if (r.textWebRef != null && r.textWeb == null) out = out.copy(textWeb = pool[r.textWebRef])
                    out
                }
            )
        }
        return file.copy(days = days)
    }

    private fun textsPool(locale: String): Map<String, String> = textsCache.getOrPut(locale) {
        try {
            // Stream-decode: the RU pool is ~38 MB; readText() would briefly double it.
            context.assets.open("localization/texts_${locale}.json").use {
                json.decodeFromStream<Map<String, String>>(it)
            }
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private fun decodeAsset(key: String): CalendarFile? = try {
        context.assets.open("localization/$key.json").use { json.decodeFromStream<CalendarFile>(it) }
    } catch (e: Exception) {
        null
    }

    // MARK: - Localization (bundled)

    suspend fun loadLocalizationBundle(localeFile: String): LocalizationBundle? =
        withContext(Dispatchers.IO) {
            val filename = "localization/${localeFile}.json"
            try {
                val jsonString = context.assets.open(filename).bufferedReader().use { it.readText() }
                json.decodeFromString<LocalizationBundle>(jsonString)
            } catch (e: Exception) {
                null
            }
        }
}

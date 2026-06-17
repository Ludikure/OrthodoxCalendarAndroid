package com.orthodox.calendar.data.repository

import android.content.Context
import com.orthodox.calendar.data.model.CalendarDay
import com.orthodox.calendar.data.model.CalendarFile
import com.orthodox.calendar.data.model.LocalizationBundle
import com.orthodox.calendar.data.network.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException

/**
 * Loads a year of calendar data for a given locale, resolving in order:
 *   1. in-memory cache (avoid re-decoding)
 *   2. app bundle / assets (all years currently ship bundled)
 *   3. on-disk cache (a year fetched earlier, kept for offline use)
 *   4. network (the Cloudflare Worker backed by R2)
 *
 * Mirror of iOS `App/CalendarRepository.swift`. Network results are persisted to
 * the cache directory so a year stays available offline once viewed. Because the
 * current years are bundled, "today" never depends on the network.
 */
class CalendarRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }
    private val cache = mutableMapOf<String, CalendarFile>()
    /** Per-locale deduped bio text pool (bios_<locale>.json), loaded lazily. */
    private val biosCache = mutableMapOf<String, Map<String, String>>()

    /** Public API base. Mirrors the bundle data via R2. */
    private val baseUrl = "https://orthodox-calendar-api.ludikure.workers.dev"

    sealed class LoadError : Exception() {
        /** No/failed connection and nothing cached. */
        object Offline : LoadError()
        /** Server has no data for this locale/year. */
        object NotFound : LoadError()
    }

    private fun fileKey(locale: String, year: Int) = "calendar_${locale}_${year}"

    /** Days for a single month; throws [LoadError] when the year cannot be resolved. */
    suspend fun loadMonth(locale: String, year: Int, month: Int): List<CalendarDay> {
        val file = load(locale, year)
        val prefix = "%02d-".format(month)
        return file.days
            .filter { it.key.startsWith(prefix) }
            .entries
            .sortedBy { it.key }
            .map { it.value }
    }

    /**
     * Resolve a year, fetching and caching from the network only when it is
     * neither bundled nor already on disk.
     */
    suspend fun load(locale: String, year: Int): CalendarFile {
        val key = fileKey(locale, year)

        cache[key]?.let { return it }

        (decode(bundleData(key)) ?: decode(diskData(key)))?.let { file ->
            val resolved = resolveBios(file, locale)
            cache[key] = resolved
            return resolved
        }

        val file = resolveBios(fetch(locale, year, key), locale)
        cache[key] = file
        return file
    }

    /**
     * Fills [SaintBio.text] from the per-locale pool for deduped bundled data.
     * No-op for API-streamed data (bios already carry their text, no `ref`).
     */
    private fun resolveBios(file: CalendarFile, locale: String): CalendarFile {
        val needs = file.days.values.any { d -> d.saintBios?.any { it.ref != null } == true }
        if (!needs) return file
        val pool = biosPool(locale)
        val days = file.days.mapValues { (_, day) ->
            val bios = day.saintBios ?: return@mapValues day
            day.copy(saintBios = bios.map { b ->
                if (b.ref != null && b.text.isEmpty()) b.copy(text = pool[b.ref] ?: "") else b
            })
        }
        return file.copy(days = days)
    }

    private fun biosPool(locale: String): Map<String, String> = biosCache.getOrPut(locale) {
        try {
            val s = context.assets.open("localization/bios_${locale}.json")
                .bufferedReader().use { it.readText() }
            json.decodeFromString<Map<String, String>>(s)
        } catch (e: Exception) {
            emptyMap()
        }
    }

    // MARK: - Sources

    private fun bundleData(key: String): String? = try {
        context.assets.open("localization/$key.json").bufferedReader().use { it.readText() }
    } catch (e: Exception) {
        null
    }

    private fun diskData(key: String): String? =
        diskFile(key).takeIf { it.exists() }?.let {
            try {
                it.readText()
            } catch (e: Exception) {
                null
            }
        }

    private suspend fun fetch(locale: String, year: Int, key: String): CalendarFile {
        val response = try {
            ApiClient.get("$baseUrl/api/$locale/$year")
        } catch (e: IOException) {
            throw LoadError.Offline
        }

        if (response.statusCode != 200) throw LoadError.NotFound
        val file = decode(response.body) ?: throw LoadError.NotFound

        // Persist the raw bytes for offline reuse; failure here is non-fatal.
        try {
            diskFile(key).writeText(response.body)
        } catch (e: Exception) {
            // ignore
        }
        return file
    }

    // MARK: - Disk cache

    private fun cacheDirectory(): File =
        File(context.cacheDir, "calendar").apply { mkdirs() }

    private fun diskFile(key: String): File = File(cacheDirectory(), "$key.json")

    private fun decode(jsonString: String?): CalendarFile? {
        if (jsonString == null) return null
        return try {
            json.decodeFromString<CalendarFile>(jsonString)
        } catch (e: Exception) {
            null
        }
    }

    // MARK: - Localization (bundled only)

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

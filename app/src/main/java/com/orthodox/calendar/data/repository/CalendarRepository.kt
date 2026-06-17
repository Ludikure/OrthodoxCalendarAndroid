package com.orthodox.calendar.data.repository

import android.content.Context
import com.orthodox.calendar.data.model.CalendarDay
import com.orthodox.calendar.data.model.CalendarFile
import com.orthodox.calendar.data.model.LocalizationBundle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

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
@OptIn(ExperimentalSerializationApi::class)
class CalendarRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }
    private val cache = mutableMapOf<String, CalendarFile>()
    /** Per-locale deduped bio text pool (bios_<locale>.json), loaded lazily. */
    private val biosCache = mutableMapOf<String, Map<String, String>>()
    /** Serializes uncached year loads so concurrent navigation can't run several
     *  large (~50 MB) network/decode operations at once and exhaust the heap. */
    private val loadMutex = Mutex()

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

        // Only one uncached load at a time: a newer navigation cancels its waiter
        // here (see ViewModel) so we never decode several big years concurrently.
        return loadMutex.withLock {
            cache[key]?.let { return@withLock it }
            val file = withContext(Dispatchers.IO) {
                (decodeAsset(key) ?: decodeDisk(key))?.let { resolveBios(it, locale) }
                    ?: resolveBios(fetch(locale, year, key), locale)
            }
            cache[key] = file
            file
        }
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
            // Stream-decode: the RU pool is ~36 MB; readText() would briefly double it.
            context.assets.open("localization/bios_${locale}.json").use {
                json.decodeFromStream<Map<String, String>>(it)
            }
        } catch (e: Exception) {
            emptyMap()
        }
    }

    // MARK: - Sources (stream-decode everywhere so a ~50 MB year never becomes a String)

    private fun decodeAsset(key: String): CalendarFile? = try {
        context.assets.open("localization/$key.json").use { json.decodeFromStream<CalendarFile>(it) }
    } catch (e: Exception) {
        null
    }

    private fun decodeDisk(key: String): CalendarFile? {
        val f = diskFile(key)
        if (!f.exists()) return null
        return try {
            f.inputStream().use { json.decodeFromStream<CalendarFile>(it) }
        } catch (e: Exception) {
            null
        }
    }

    private fun fetch(locale: String, year: Int, key: String): CalendarFile {
        val conn = (URL("$baseUrl/api/$locale/$year").openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 60_000
        }
        try {
            if (conn.responseCode != 200) throw LoadError.NotFound
            // Stream the response straight to disk (constant memory), then decode
            // from the file as a stream — avoids holding the whole 50 MB in memory.
            conn.inputStream.use { input ->
                diskFile(key).outputStream().use { output -> input.copyTo(output) }
            }
            return decodeDisk(key) ?: throw LoadError.NotFound
        } catch (e: LoadError) {
            throw e
        } catch (e: IOException) {
            throw LoadError.Offline
        } finally {
            conn.disconnect()
        }
    }

    // MARK: - Disk cache

    private fun cacheDirectory(): File =
        File(context.cacheDir, "calendar").apply { mkdirs() }

    private fun diskFile(key: String): File = File(cacheDirectory(), "$key.json")

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

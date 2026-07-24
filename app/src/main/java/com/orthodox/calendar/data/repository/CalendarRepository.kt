package com.orthodox.calendar.data.repository

import android.content.Context
import com.orthodox.calendar.data.model.CalendarDay
import com.orthodox.calendar.data.model.CalendarFile
import com.orthodox.calendar.data.model.LocalizationBundle
import com.orthodox.calendar.data.network.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.io.File

/**
 * Loads a year of calendar data for a given locale.
 *
 * Bundled years (assets/localization, the window copied from the iOS repo)
 * resolve offline, exactly as before. Years outside the bundle come from the
 * v2 archive on the Cloudflare Worker (deduplicated files, 2024-2099) and are
 * cached permanently under filesDir/calendar_cache (excluded from backup), so
 * each is downloaded at most once. Large text (saint bios + scripture
 * readings) lives in a per-locale `texts_<locale>` pool keyed by content hash;
 * bundled and downloaded files alike reference it. `/api/config`'s
 * `dataRevision` invalidates the disk cache when the archive is regenerated.
 * Mirror of iOS `CalendarRepository`.
 */
@OptIn(ExperimentalSerializationApi::class)
class CalendarRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }
    private val cache = mutableMapOf<String, CalendarFile>()
    /** Per-locale deduped text pool (texts_<locale>.json), loaded lazily. */
    private val textsCache = mutableMapOf<String, Map<String, String>>()
    /** Config revision is checked at most once per process, and only on the
     *  network path — bundled years never touch the network. */
    private var revisionChecked = false

    sealed class LoadError : Exception() {
        /** No data exists for this locale/year. */
        object NotFound : LoadError()
        /** Connectivity problem; retry may succeed. */
        object Offline : LoadError()
    }

    private fun fileKey(locale: String, year: Int) = "calendar_${locale}_${year}"

    /** Days for a single month; throws [LoadError] when the year can't load. */
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
     * Resolve a year: memory → assets → disk cache → network (cached to disk).
     *
     * [allowNetwork] false stops after the disk cache — used for neighbour
     * years in season-span computation, which must never block a month render
     * on a download.
     */
    suspend fun load(locale: String, year: Int, allowNetwork: Boolean = true): CalendarFile {
        val key = fileKey(locale, year)
        cache[key]?.let { return it }
        var raw = withContext(Dispatchers.IO) { decodeAsset(key) ?: decodeDisk(key) }
        if (raw == null) {
            if (!allowNetwork) throw LoadError.NotFound
            raw = download(locale, year, key)
        }
        val file = withContext(Dispatchers.IO) { resolveText(raw, locale) }
        cache[key] = file
        return file
    }

    // MARK: - Network

    private suspend fun download(locale: String, year: Int, key: String): CalendarFile {
        checkRevisionOnce()
        val response = try {
            ApiClient.get("$API_BASE/$locale/$year")
        } catch (e: Exception) {
            throw LoadError.Offline
        }
        when (response.statusCode) {
            200 -> Unit
            400, 404 -> throw LoadError.NotFound
            else -> throw LoadError.Offline
        }
        val file = try {
            json.decodeFromString<CalendarFile>(response.body)
        } catch (e: Exception) {
            throw LoadError.NotFound
        }
        withContext(Dispatchers.IO) {
            runCatching {
                cacheDir().mkdirs()
                File(cacheDir(), "$key.json").writeText(response.body)
            }
        }
        return file
    }

    /**
     * Drops the disk cache when the server's archive revision moves past the
     * one our cached files were downloaded under. Fails open: no connectivity
     * or a malformed config leaves the cache as is.
     */
    private suspend fun checkRevisionOnce() {
        if (revisionChecked) return
        revisionChecked = true
        val revision = try {
            val response = ApiClient.get(CONFIG_URL)
            if (response.statusCode != 200) return
            json.decodeFromString<WorkerConfig>(response.body).dataRevision ?: return
        } catch (e: Exception) {
            return
        }
        withContext(Dispatchers.IO) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val stored = prefs.getInt(REVISION_KEY, 0)
            if (stored != 0 && stored != revision) {
                runCatching { cacheDir().deleteRecursively() }
            }
            prefs.edit().putInt(REVISION_KEY, revision).apply()
        }
    }

    @Serializable
    private data class WorkerConfig(val dataRevision: Int? = null)

    // MARK: - Disk cache

    private fun cacheDir() = File(context.filesDir, "calendar_cache")

    private fun decodeDisk(key: String): CalendarFile? {
        val file = File(cacheDir(), "$key.json")
        if (!file.exists()) return null
        return try {
            file.inputStream().use { json.decodeFromStream<CalendarFile>(it) }
        } catch (e: Exception) {
            file.delete() // corrupted cache entry — refetch next time
            null
        }
    }

    // MARK: - Text pool resolution

    /** Fills bio + reading text from the per-locale pool for deduped data. */
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

    companion object {
        private const val API_BASE = "https://orthodox-calendar-api.ludikure.workers.dev/api/v2"
        private const val CONFIG_URL = "https://orthodox-calendar-api.ludikure.workers.dev/api/config"
        private const val PREFS_NAME = "calendar_cache_prefs"
        private const val REVISION_KEY = "cachedDataRevision"
    }
}

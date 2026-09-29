package com.orthodox.calendar.data.repository

import android.content.Context
import com.orthodox.calendar.data.model.CalendarDay
import com.orthodox.calendar.data.model.CalendarFile
import com.orthodox.calendar.data.network.ApiClient
import androidx.core.content.edit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.orthodox.calendar.data.repository.YearSource.LoadError
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.io.File
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * Loads a year of calendar data for a given locale.
 *
 * Bundled years (assets/localization, the window copied from the iOS repo)
 * resolve offline. When the archive has been republished since the bundle was
 * built (`/api/config`'s `dataRevision` above [BundledData.REVISION]), each
 * bundled year is downloaded once in the background, cached as
 * `calendar_<locale>_<year>.r<rev>.json`, and preferred over the bundle from
 * then on — see [BundledData]. Years outside the bundle come from the
 * v2 archive on the Cloudflare Worker (deduplicated files, 2024-2099) and are
 * cached permanently under filesDir/calendar_cache (excluded from backup), so
 * each is downloaded at most once. Large text (saint bios + scripture
 * readings) lives in a per-locale `texts_<locale>` pool keyed by content hash;
 * bundled and downloaded files alike reference it. `/api/config`'s
 * `dataRevision` invalidates the disk cache when the archive is regenerated.
 * Mirror of iOS `CalendarRepository`.
 */
@OptIn(ExperimentalSerializationApi::class)
class CalendarRepository(private val context: Context) : YearSource {

    private val json = Json { ignoreUnknownKeys = true }
    /* One repository serves every screen, and its callers are spread across
     * dispatchers: `load` is entered from the ViewModel's Main-bound scope while
     * `loadUncached` writes from Dispatchers.IO, and the revision check clears
     * both maps from a third. Plain HashMaps here were a data race — concurrent
     * writes can corrupt the table outright, not merely lose an entry; the LRU
     * below guards itself with a monitor instead. */
    private val cache = BoundedCache<String, CalendarFile>(MAX_CACHED_YEARS)
    /** Loads in flight, so two screens asking for the same uncached year share
     *  one download and one resolveText pass instead of racing. Guarded by
     *  [inFlightLock]: the critical sections are map lookups with no suspension
     *  point, and the completion handler must be able to take the lock without
     *  suspending. */
    private val inFlight = mutableMapOf<String, Deferred<CalendarFile>>()
    private val inFlightLock = Any()
    /** Per-locale deduped text pool (texts_<locale>.json), loaded lazily. */
    private val textsCache = ConcurrentHashMap<String, Map<String, String>>()
    /** Config revision is checked at most once per process, and only on the
     *  network path — bundled years never touch the network. */
    @Volatile private var revisionChecked = false
    /** The archive revision `/api/config` reported this process, once checked. */
    @Volatile private var serverRevision: Int? = null
    /** Bundled years whose newer archive copy is being fetched right now. */
    private val refreshing = ConcurrentHashMap.newKeySet<String>()
    /** The calendar files in assets/localization, listed once. */
    private val bundledNames: Set<String> by lazy {
        runCatching { context.assets.list("localization")?.toSet() }.getOrNull().orEmpty()
    }
    /**
     * Bumped when the archive revision moves and the caches are dropped. A load
     * that read its year from the disk cache before the bump holds superseded
     * data and must not put it back in memory — see [storeIfCurrent]. Changed
     * only under [generationLock], together with the clear, so a store cannot
     * land between the two.
     */
    @Volatile private var cacheGeneration = 0
    private val generationLock = Any()
    /** Scope for shared loads; process-lived, like the repository itself. */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private fun fileKey(locale: String, year: Int) = "calendar_${locale}_${year}"

    /**
     * The in-flight key carries `allowNetwork` because the job body closes over
     * it. Keyed on locale+year alone, a caller that passed `allowNetwork = false`
     * (the neighbour year of a fasting-season span) could hand a network-allowed
     * caller its `NotFound` instead of the year it was willing to download, and
     * the month would then read "No calendar data for 2031" with a retry button
     * that works the moment you leave and come back.
     */
    private fun inFlightKey(locale: String, year: Int, allowNetwork: Boolean) =
        "${fileKey(locale, year)}:${if (allowNetwork) "net" else "local"}"

    /** Days for a single month; throws [LoadError] when the year can't load. */
    override suspend fun loadMonth(locale: String, year: Int, month: Int): List<CalendarDay> {
        val file = load(locale, year)
        val prefix = monthKeyPrefix(month)
        return file.days
            .filter { it.key.startsWith(prefix) }
            .entries
            .sortedBy { it.key }
            .map { it.value }
    }

    /**
     * Resolve a year: memory → assets (or their newer archive copy) → disk
     * cache → network (cached to disk).
     *
     * [allowNetwork] false stops after the disk cache — used for neighbour
     * years in season-span computation, which must never block a month render
     * on a download.
     */
    override suspend fun load(locale: String, year: Int, allowNetwork: Boolean): CalendarFile {
        val key = fileKey(locale, year)
        cache[key]?.let { return it }
        val flightKey = inFlightKey(locale, year, allowNetwork)
        // Join an existing load rather than starting a second one. The shared
        // job runs outside any one caller's scope so that a caller giving up
        // does not cancel the load the others are still waiting on.
        var started: Deferred<CalendarFile>? = null
        val job = synchronized(inFlightLock) {
            inFlight[flightKey] ?: scope.async(start = CoroutineStart.LAZY) {
                loadUncached(locale, year, key, allowNetwork)
            }.also {
                inFlight[flightKey] = it
                started = it
            }
        }
        // Cleanup belongs to the job, not to whichever caller returns first.
        // Removing the entry in the caller's `finally` meant that the first
        // caller to be cancelled — a month swipe cancels the ViewModel's load
        // job on every navigation — deleted the entry while the others were
        // still awaiting it, so the next request started a second download and
        // a second resolveText pass over the same 17 MB pool. That is exactly
        // the duplicate this dedup exists to prevent.
        //
        // The job is created LAZY so it cannot finish before the handler is
        // attached and fire it inline, mid-`synchronized`, on the map we are
        // about to unlock.
        started?.let { fresh ->
            fresh.invokeOnCompletion {
                synchronized(inFlightLock) { if (inFlight[flightKey] === fresh) inFlight.remove(flightKey) }
            }
            fresh.start()
        }
        return job.await()
    }

    /**
     * Drops everything the repository holds in memory: decoded years and text
     * pools. Called when the system reports memory pressure
     * (`Application.onTrimMemory`); every one of these is re-readable from
     * assets or the disk cache, so nothing here is lost, only re-fetched.
     *
     * Years and pools go together on purpose. A resolved year's reading and
     * biography strings *are* the pool's strings, so clearing one map while the
     * other is kept pins both in memory and frees nothing.
     */
    fun releaseMemory() {
        cache.clear()
        textsCache.clear()
        // Not `inFlight`: a job that is mid-download would simply re-insert its
        // year when it finishes, which is correct — that year is the one on
        // screen. Clearing the map instead would strand the callers waiting on
        // an entry nobody owns.
    }

    private suspend fun loadUncached(
        locale: String, year: Int, key: String, allowNetwork: Boolean
    ): CalendarFile {
        // Taken before the disk read: that read is what a revision change can
        // supersede while this load is still resolving it.
        val startGeneration = cacheGeneration
        val bundled = "$key.json" in bundledNames
        val local = withContext(Dispatchers.IO) {
            if (bundled) {
                // A bundled year: its archive copy only when that is newer than
                // the bundle. Read from disk, so a revision bump that lands
                // mid-read keeps it out of memory like any disk read.
                newerCopy(key)
                    ?.takeIf { BundledData.source(BundledData.REVISION, it.second) == BundledData.Source.CACHE }
                    ?.let { decodeDisk(BundledData.cacheName(key, it.second)) }
                    ?.let { it to true }
                    ?: decodeAsset(key)?.let { it to false }
            } else {
                decodeAsset(key)?.let { it to false } ?: decodeDisk(key)?.let { it to true }
            }
        }
        // Never a network wait for a bundled year: the refresh runs behind, and
        // the next load picks its result up.
        if (bundled && allowNetwork) refreshBundledYear(locale, year, key)
        val raw = local?.first ?: run {
            if (!allowNetwork) throw LoadError.NotFound
            download(locale, year, key)
        }
        val file = withContext(Dispatchers.IO) { resolveText(raw, locale) }
        storeIfCurrent(key, file, fromDisk = local?.second == true, startGeneration = startGeneration)
        return file
    }

    /**
     * Puts a resolved year in memory — unless it was read from the disk cache
     * before the archive revision moved. Such a load finishes after
     * [invalidateForNewRevision] has emptied the caches, and storing it would
     * serve the superseded year from memory for the life of the process: the
     * stale data the revision exists to throw away. Bundled years come from the
     * APK and downloads are fetched after the check, so neither can be stale.
     */
    internal fun storeIfCurrent(key: String, file: CalendarFile, fromDisk: Boolean, startGeneration: Int) {
        synchronized(generationLock) {
            if (fromDisk && cacheGeneration != startGeneration) return
            cache[key] = file
        }
    }

    /** The generation a load starting now would run under. For tests. */
    internal fun currentGeneration(): Int = cacheGeneration

    /**
     * Drops what the previous archive revision left: the downloaded years on
     * disk, and every decoded year and text pool in memory — the repository
     * outlives the activity, so clearing only the disk would keep superseded
     * years on screen until the process restarts.
     *
     * Loads already in flight keep running, since callers are waiting on them;
     * one that read its year from disk before this point will not store it
     * ([storeIfCurrent]). That replaces clearing `inFlight` here, which did not
     * do what it said: a running load never consults `inFlight` before writing
     * its result, so the stale year went back into memory regardless, and a
     * caller arriving afterwards started a duplicate load instead of joining it.
     */
    internal fun invalidateForNewRevision() {
        runCatching { cacheDir().deleteRecursively() }
        synchronized(generationLock) {
            cacheGeneration++
            cache.clear()
            textsCache.clear()
        }
    }

    // MARK: - Newer data for bundled years

    /**
     * Fetches a bundled year from the archive when `/api/config` says the archive
     * is newer than the bundle and no copy at that revision is on disk. Runs
     * detached from the load that started it: offline, a failed request or an
     * unchanged revision leave the bundle in use and say nothing.
     */
    private fun refreshBundledYear(locale: String, year: Int, key: String) {
        if (!refreshing.add(key)) return
        scope.launch(Dispatchers.IO) {
            try {
                checkRevisionOnce()
                val revision = serverRevision ?: return@launch
                if (!BundledData.shouldDownload(BundledData.REVISION, revision, newerCopy(key)?.second)) return@launch
                val response = ApiClient.get("$API_BASE/$locale/$year")
                if (response.statusCode != 200) return@launch
                val file = json.decodeFromString<CalendarFile>(response.body)
                // Written only if the revision has not moved again meanwhile: a
                // copy named for a superseded revision would win over the bundle.
                if (serverRevision != revision) return@launch
                cacheDir().mkdirs()
                File(cacheDir(), BundledData.cacheName(key, revision) + ".json").writeText(response.body)
                // Loads from here on get the new copy; a year already in memory
                // is swapped too, so the next month rendered from it is fresh.
                if (cache[key] != null) {
                    val resolved = resolveText(file, locale)
                    synchronized(generationLock) { if (cache[key] != null) cache[key] = resolved }
                }
            } catch (c: CancellationException) {
                throw c
            } catch (e: Exception) {
                // Offline or a bad response: the bundle stays in use.
            } finally {
                refreshing.remove(key)
            }
        }
    }

    /**
     * The revision of the newest archive copy of bundled year [key] on disk, with
     * its file. Copies at or below the bundle's revision can never win, so they
     * are deleted here rather than left for the next revision bump.
     */
    private fun newerCopy(key: String): Pair<File, Int>? {
        val copies = cacheDir().listFiles().orEmpty()
            .mapNotNull { f -> BundledData.revision(f.name, key)?.let { f to it } }
        copies.filter { it.second <= BundledData.REVISION }.forEach { runCatching { it.first.delete() } }
        return copies.filter { it.second > BundledData.REVISION }.maxByOrNull { it.second }
    }

    // MARK: - Network

    private suspend fun download(locale: String, year: Int, key: String): CalendarFile {
        checkRevisionOnce()
        val response = try {
            ApiClient.get("$API_BASE/$locale/$year")
        } catch (c: CancellationException) {
            // A superseded navigation cancels this load. Rewriting that as
            // Offline made the ViewModel's own CancellationException guard
            // unreachable, so an abandoned load blanked the month it had
            // already been replaced by.
            throw c
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
            // A 200 that will not parse is a truncated or intercepted response —
            // a captive portal, a proxy, a dropped connection. Reporting it as
            // NotFound told the user the archive has no such year and made the
            // retry button look pointless, when retrying is exactly the fix.
            throw LoadError.Offline
        }
        withContext(Dispatchers.IO) {
            runCatching {
                cacheDir().mkdirs()
                File(cacheDir(), "$key.json").writeText(response.body)
                trimDiskCache()
            }
        }
        return file
    }

    /**
     * Keeps the [MAX_YEARS_PER_LOCALE_ON_DISK] most recently used downloaded
     * years per locale and deletes the rest — see [cacheFilesToEvict] for what
     * "recently used" means and why it is not "highest year number".
     *
     * Only years outside the bundled window ever land here, but a user paging
     * forward through the archive one year at a time left every one of them on
     * disk for the life of the install — the directory has no size limit and
     * nothing removed a file until the archive revision moved. The whole
     * directory is excluded from backup and every file re-downloadable, so the
     * cost of a deletion is one fetch for a year the user has not opened in
     * months; the cost of not deleting is hundreds of megabytes.
     *
     * Trimmed after a write rather than on a schedule: the year just written is
     * the most recent by definition, and this runs once per download.
     */
    private fun trimDiskCache() {
        runCatching {
            val files = cacheDir().listFiles()?.toList() ?: return
            cacheFilesToEvict(files, MAX_YEARS_PER_LOCALE_ON_DISK).forEach { it.delete() }
        }
    }

    /**
     * Drops the disk cache when the server's archive revision moves past the
     * one our cached files were downloaded under. Fails open: no connectivity
     * or a malformed config leaves the cache as is. Archive copies of bundled
     * years go with the rest, so a bump never leaves an older copy preferred:
     * until the new one lands the bundle is used, and a copy is taken over the
     * bundle only when its own revision (in its file name) is newer.
     */
    private suspend fun checkRevisionOnce() {
        if (revisionChecked) return
        val revision = try {
            val response = ApiClient.get(CONFIG_URL)
            if (response.statusCode != 200) return
            json.decodeFromString<WorkerConfig>(response.body).dataRevision ?: return
        } catch (c: CancellationException) {
            throw c
        } catch (e: Exception) {
            return
        }
        serverRevision = revision
        withContext(Dispatchers.IO) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val stored = prefs.getInt(REVISION_KEY, 0)
            if (stored != 0 && stored != revision) invalidateForNewRevision()
            prefs.edit { putInt(REVISION_KEY, revision) }
        }
        // Latched only now: a first launch without connectivity should retry on
        // the next download rather than skip the check for the whole process.
        revisionChecked = true
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
                // A read is a use: the disk trim keeps the most recently used
                // years, and without this a year opened every day but downloaded
                // long ago would be the first to go.
                .also { file.setLastModified(System.currentTimeMillis()) }
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

    /**
     * The pool file a locale resolves against. en and en_nc show the same bios
     * and the same scripture text, so they share `texts_en.json` — bundling a
     * second, byte-identical copy cost 3 MB of assets.
     */
    internal fun poolName(locale: String) = if (locale == "en_nc") "en" else locale

    private fun textsPool(locale: String): Map<String, String> {
        val name = poolName(locale)
        textsCache[name]?.let { return it }
        return try {
            // Stream-decode: the RU pool is ~17 MB; readText() would briefly double it.
            context.assets.open("localization/texts_$name.json").use {
                json.decodeFromStream<Map<String, String>>(it)
            }.also { textsCache[name] = it }   // only a pool that loaded is cached
        } catch (e: Exception) {
            // Deliberately not cached. Storing the empty map — which getOrPut did —
            // meant one transient failure on the 17 MB pool blanked every saint
            // life and every scripture text for that locale for the life of the
            // process, silently and with no way back but a restart.
            emptyMap()
        }
    }

    private fun decodeAsset(key: String): CalendarFile? = try {
        context.assets.open("localization/$key.json").use { json.decodeFromStream<CalendarFile>(it) }
    } catch (e: Exception) {
        null
    }

    /** Keys of the years held in memory, oldest first. For tests and memory
     *  reporting; the cache is bounded, so this list never grows past it. */
    internal fun cachedYearKeys(): List<String> = cache.keys()

    /** The key prefix one month's days share inside a calendar file
     *  (`"01-"` … `"12-"`). Fixed-locale formatting: under a locale with
     *  non-ASCII digits the prefix would match nothing and the month would
     *  render empty. */
    internal fun monthKeyPrefix(month: Int): String = "%02d-".format(Locale.ROOT, month)

    companion object {
        /** Decoded years kept in memory. See [BoundedCache]. */
        private const val MAX_CACHED_YEARS = 6

        /** Downloaded years kept on disk per locale. See [trimDiskCache]. */
        private const val MAX_YEARS_PER_LOCALE_ON_DISK = 12

        private const val API_BASE = "https://orthodox-calendar-api.ludikure.workers.dev/api/v2"
        private const val CONFIG_URL = "https://orthodox-calendar-api.ludikure.workers.dev/api/config"
        private const val PREFS_NAME = "calendar_cache_prefs"
        private const val REVISION_KEY = "cachedDataRevision"
    }
}

/**
 * Which copy of a bundled year to open: the bundle, or an archive copy
 * downloaded because the archive was republished after the bundle was built.
 *
 * [REVISION] is the archive `dataRevision` (worker/config.json in the iOS repo)
 * the bundled calendar_*.json assets were generated for. RELEASE CHECKLIST: when
 * regenerated years are copied into assets/localization, set it to the
 * `dataRevision` they are published under — the same value as iOS
 * `BundledData.revision` for the same files. Too low and every install
 * re-downloads years its bundle already has; too high and it never picks up
 * corrections.
 *
 * Mirror of `BundledData` in the iOS `CalendarRepository.swift`.
 */
object BundledData {
    const val REVISION = 9

    enum class Source { BUNDLE, CACHE }

    /**
     * The archive copy wins only when it is newer than the bundle: a copy from
     * before a newer app shipped (its revision at or below the bundle's) is
     * stale, and none at all means the bundle.
     */
    fun source(bundleRevision: Int, cachedRevision: Int?): Source =
        if (cachedRevision != null && cachedRevision > bundleRevision) Source.CACHE else Source.BUNDLE

    /**
     * Download when the server's archive is newer than the bundle and newer than
     * any copy already on disk. An unknown server revision (offline, a bad
     * config) never downloads.
     */
    fun shouldDownload(bundleRevision: Int, serverRevision: Int?, cachedRevision: Int?): Boolean {
        if (serverRevision == null || serverRevision <= bundleRevision) return false
        return (cachedRevision ?: 0) < serverRevision
    }

    /**
     * Disk-cache key of a bundled year's archive copy: the revision rides in the
     * name ("calendar_ru_2026.r10"), so no copy is ever mistaken for another
     * revision's. The disk trim (`cacheFilesToEvict`) does not count these.
     */
    fun cacheName(key: String, revision: Int): String = "$key.r$revision"

    /** The revision in [fileName] when it is an archive copy of [key], else null. */
    fun revision(fileName: String, key: String): Int? {
        val prefix = "$key.r"
        val suffix = ".json"
        if (!fileName.startsWith(prefix) || !fileName.endsWith(suffix)) return null
        return fileName.substring(prefix.length, fileName.length - suffix.length).toIntOrNull()
    }
}

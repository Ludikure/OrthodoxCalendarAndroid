package com.orthodox.calendar.app

import com.orthodox.calendar.BuildConfig
import com.orthodox.calendar.data.network.ApiClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Server-controlled minimum-version gate.
 *
 * On launch the app fetches `/api/config`; if the installed version is below the
 * server's `minVersion`, the app shows a blocking update screen. The minimum is
 * controlled server-side, so an update can be forced later by changing one value
 * — no new app release required.
 *
 * Fail-open by design: any network/parse failure leaves `mustUpdate == false`,
 * so offline users of this offline-first app are never blocked.
 *
 * Mirror of iOS `App/AppUpdateGate.swift`.
 */
class AppUpdateGate {

    private val _mustUpdate = MutableStateFlow(false)
    val mustUpdate: StateFlow<Boolean> = _mustUpdate.asStateFlow()

    /** Resolved at check() time: a `playStoreUrl` if the server supplies one.
     *  A StateFlow rather than a plain var — it is written from [check]'s
     *  dispatcher and read from the composition. */
    private val _storeUrl = MutableStateFlow<String?>(null)
    val storeUrl: StateFlow<String?> = _storeUrl.asStateFlow()

    /** The gate is process-scoped now, so a rotation must not refetch. Latched
     *  only on success: a launch with no connectivity should ask again. */
    @Volatile private var checked = false

    private val configUrl = "https://orthodox-calendar-api.ludikure.workers.dev/api/config"
    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    private data class Config(
        val minVersion: String,
        val appStoreUrl: String? = null,
        val playStoreUrl: String? = null
    )

    val installedVersion: String get() = BuildConfig.VERSION_NAME

    private fun isStoreUrl(url: String): Boolean = runCatching {
        val uri = java.net.URI(url)
        uri.scheme == "https" && uri.host in setOf("play.google.com", "market.android.com")
    }.getOrDefault(false)

    suspend fun check() {
        if (checked) return
        val config = try {
            val response = ApiClient.get(configUrl)
            if (response.statusCode != 200) return  // fail-open
            json.decodeFromString<Config>(response.body)
        } catch (c: CancellationException) {
            throw c  // the screen went away; not a config failure
        } catch (e: Exception) {
            return  // fail-open: never block on a failed/edge-cached miss
        }

        // Prefer a Play Store URL; fall back to the Play listing by package id.
        // A server-supplied URL is only followed when it points at the store;
        // anything else falls back to this app's own listing.
        val fallback = "https://play.google.com/store/apps/details?id=${BuildConfig.APPLICATION_ID}"
        _storeUrl.value = config.playStoreUrl?.takeIf { isStoreUrl(it) } ?: fallback

        if (isOlder(installedVersion, config.minVersion)) {
            _mustUpdate.value = true
        }
        checked = true
    }

    companion object {
        /**
         * True if [version] is strictly older than [minimum] (dotted numeric
         * compare, e.g. "1.3.0" < "1.4.0"). Missing components count as 0, so
         * "1.4" == "1.4.0". A version that does not parse is *not* older: the
         * gate is fail-open by design, and treating an unparseable component as
         * 0 made every such version older than any real minimum — a malformed
         * versionName would have walled a working app behind the update screen.
         * Identical to iOS `AppUpdateGate.isOlder`.
         */
        fun isOlder(version: String, minimum: String): Boolean {
            val a = components(version) ?: return false
            val b = components(minimum) ?: return false
            for (i in 0 until maxOf(a.size, b.size)) {
                val x = a.getOrElse(i) { 0 }
                val y = b.getOrElse(i) { 0 }
                if (x != y) return x < y
            }
            return false
        }

        private fun components(version: String): List<Int>? {
            val parts = version.split(".")
            if (parts.isEmpty()) return null
            return parts.map { it.toIntOrNull() ?: return null }
        }
    }
}

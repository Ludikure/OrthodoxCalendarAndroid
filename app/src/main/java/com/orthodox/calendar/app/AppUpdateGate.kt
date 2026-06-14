package com.orthodox.calendar.app

import com.orthodox.calendar.BuildConfig
import com.orthodox.calendar.data.network.ApiClient
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

    /** Resolved at check() time: a `playStoreUrl` if the server supplies one. */
    var storeUrl: String? = null
        private set

    private val configUrl = "https://orthodox-calendar-api.ludikure.workers.dev/api/config"
    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    private data class Config(
        val minVersion: String,
        val appStoreUrl: String? = null,
        val playStoreUrl: String? = null
    )

    val installedVersion: String get() = BuildConfig.VERSION_NAME

    suspend fun check() {
        val config = try {
            val response = ApiClient.get(configUrl)
            if (response.statusCode != 200) return  // fail-open
            json.decodeFromString<Config>(response.body)
        } catch (e: Exception) {
            return  // fail-open: never block on a failed/edge-cached miss
        }

        // Prefer a Play Store URL; fall back to the Play listing by package id.
        storeUrl = config.playStoreUrl
            ?: "https://play.google.com/store/apps/details?id=${BuildConfig.APPLICATION_ID}"

        if (isOlder(installedVersion, config.minVersion)) {
            _mustUpdate.value = true
        }
    }

    companion object {
        /**
         * True if [version] is strictly older than [minimum] (dotted numeric
         * compare, e.g. "1.3.0" < "1.4.0"). Non-numeric/missing components → 0.
         * Identical to iOS `AppUpdateGate.isOlder`.
         */
        fun isOlder(version: String, minimum: String): Boolean {
            val a = version.split(".").map { it.toIntOrNull() ?: 0 }
            val b = minimum.split(".").map { it.toIntOrNull() ?: 0 }
            for (i in 0 until maxOf(a.size, b.size)) {
                val x = a.getOrElse(i) { 0 }
                val y = b.getOrElse(i) { 0 }
                if (x != y) return x < y
            }
            return false
        }
    }
}

package com.orthodox.calendar.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Minimal dependency-free HTTP GET helper (mirrors iOS's use of `URLSession`).
 * Runs on [Dispatchers.IO]; throws [IOException] on transport failure.
 */
object ApiClient {

    data class Response(val statusCode: Int, val body: String)

    private const val CONNECT_TIMEOUT_MS = 15_000
    // v2 archive years are ~1.4 MB (they were ~50 MB before text dedup), so a
    // minute of read timeout is no longer warranted — it only makes a dead
    // connection take that long to report.
    private const val READ_TIMEOUT_MS = 20_000

    suspend fun get(url: String): Response = withContext(Dispatchers.IO) {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
        }
        try {
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() } ?: ""
            Response(status, body)
        } finally {
            connection.disconnect()
        }
    }
}

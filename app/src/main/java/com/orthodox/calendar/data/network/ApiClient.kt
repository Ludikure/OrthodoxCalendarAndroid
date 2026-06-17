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
    // Generous read timeout: the Russian year files are ~50 MB and can take a
    // while to stream over slow mobile connections.
    private const val READ_TIMEOUT_MS = 60_000

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

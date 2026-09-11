package com.orthodox.calendar.data.preferences

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/**
 * First value of [this], or [default] when the flow fails.
 *
 * `DataStore.data` throws `IOException` when the preferences file is corrupt,
 * locked, or unreadable because the disk is full — real field failures, and
 * there is no `UserDefaults`-style silent default behind it. The ViewModel read
 * these flows with a bare `first()`, so one unreadable file killed the startup
 * coroutine before it could set a localization bundle, and every screen that
 * stops while the bundle is null drew nothing at all: a blank app with no retry
 * and no way back.
 *
 * [CancellationException] is rethrown, as everywhere else in this app — a
 * cancelled scope is not a failed read.
 */
internal suspend fun <T> Flow<T>.firstOrDefault(default: T): T = try {
    first()
} catch (c: CancellationException) {
    throw c
} catch (e: Exception) {
    default
}

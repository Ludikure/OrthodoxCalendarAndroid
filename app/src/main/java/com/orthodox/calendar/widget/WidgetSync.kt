package com.orthodox.calendar.widget

import android.content.Context
import com.orthodox.calendar.data.localization.LocalizationManager
import com.orthodox.calendar.data.model.AppLanguage
import com.orthodox.calendar.data.model.CalendarDay
import com.orthodox.calendar.data.preferences.AppPreferences
import com.orthodox.calendar.data.preferences.firstOrDefault
import com.orthodox.calendar.data.repository.YearSource
import com.orthodox.calendar.data.slava.SlavaStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate

/**
 * Keeps the widgets' snapshot current. The app calls [refresh] when the current
 * year finishes loading, on a language change, on a slava change and when it
 * comes to the foreground; each call replaces the one before it.
 *
 * Years are read with `allowNetwork = false`, so this never downloads anything
 * the calendar has not. `MainActivity` calls it only while the calendar is not
 * mid-load: the repository dedups loads per (year, network-allowed) pair, so a
 * local-only read racing the calendar's own load of the same year would decode
 * the year — and its text pool, 17 MB for Russian — a second time.
 *
 * Mirror of `WidgetSync` in `OrthodoxCalendar/App/WidgetSync.swift`.
 */
class WidgetSync(
    context: Context,
    private val slavaStore: SlavaStore,
    private val years: () -> YearSource
) {
    private val context = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val localization by lazy { LocalizationManager(this.context) }
    private var job: Job? = null
    @Volatile private var redrawn = false

    /** Rebuilds the snapshot for [language] (the saved one when null). */
    @Synchronized
    fun refresh(language: AppLanguage? = null): Job {
        job?.cancel()
        return scope.launch { rebuild(language) }.also { job = it }
    }

    private suspend fun rebuild(requested: AppLanguage?) {
        val language = requested
            ?: AppPreferences(context).languageFlow.firstOrDefault(AppLanguage.SR)
        val today = LocalDate.now()
        val locale = language.code
        val days = HashMap<String, CalendarDay>()
        load(locale, today.year)?.days?.values?.forEach { days[it.gregorianDate] = it }
        if (days.isEmpty()) return
        // Late December: the snapshot runs into next year.
        if (today.plusDays(WidgetSnapshot.DAY_COUNT - 1L).year != today.year) {
            load(locale, today.year + 1)?.days?.values?.forEach { days[it.gregorianDate] = it }
        }
        val ui = synchronized(localization) { localization.loadBundle(language).ui }
        val snapshot = WidgetSnapshotBuilder.build(
            days, today, language, ui, slavaStore.settings.value.mine, Instant.now()
        ) ?: return
        currentCoroutineContext().ensureActive()

        val file = WidgetSnapshot.file(context)
        // Unchanged content: leave the widgets alone — except once per process,
        // which also repairs a widget left on Glance's loading layout (an app
        // update can race two renders of one widget, and the loser stays on
        // the spinner until the next update).
        val old = WidgetSnapshot.read(file)
        val unchanged = old != null && old.language == snapshot.language && old.days == snapshot.days
        if (unchanged && redrawn) return
        if (!unchanged) {
            try {
                WidgetSnapshot.write(snapshot, file)
            } catch (e: Exception) {
                return
            }
        }
        redrawn = true
        TodayWidgetUpdater.updateAll(context)
    }

    private suspend fun load(locale: String, year: Int) = try {
        years().load(locale, year, allowNetwork = false)
    } catch (c: CancellationException) {
        throw c
    } catch (e: Exception) {
        null
    }
}

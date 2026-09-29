package com.orthodox.calendar

import android.app.Application
import android.content.ComponentCallbacks2
import com.orthodox.calendar.app.AppUpdateGate
import com.orthodox.calendar.app.NameDayReminders
import com.orthodox.calendar.app.SlavaReminders
import com.orthodox.calendar.app.shouldReleaseCalendarCache
import com.orthodox.calendar.data.nameday.NameDayCatalog
import com.orthodox.calendar.data.nameday.NameDayStore
import com.orthodox.calendar.data.repository.CalendarRepository
import com.orthodox.calendar.data.slava.SlavaStore
import com.orthodox.calendar.widget.WidgetSync

class OrthodoxCalendarApp : Application() {
    /**
     * One repository for the whole process.
     *
     * It caches decoded years and the per-locale text pool, and texts_ru.json
     * alone is 17 MB of JSON. A second instance meant that pool resident twice,
     * and the one built in MainActivity was rebuilt on every rotation — the
     * activity is recreated on configuration change — re-reading and re-decoding
     * it each time. That is what largeHeap has been absorbing.
     */
    /* Held as a `Lazy` rather than inlined as `by lazy` so [onTrimMemory] can
     * tell "not needed yet" from "loaded and holding decoded years": touching
     * the property to release its memory would build the very thing it is
     * trying to free in a process that never opened a calendar. */
    private val repositoryLazy = lazy { CalendarRepository(this) }

    val repository: CalendarRepository by repositoryLazy

    /**
     * The update gate lives here for the same reason.
     *
     * Held in the composition it was rebuilt with the Activity, so `mustUpdate`
     * fell back to false on every rotation: a user the server had blocked could
     * turn the phone and be inside the app while /api/config was refetched.
     * Process-scoped, the gate is asked once and its answer sticks.
     */
    val updateGate: AppUpdateGate by lazy { AppUpdateGate() }

    /**
     * The user's krsna slava and friends' slavas (Serbian only), and the
     * reminders scheduled from them. Process-scoped like the repository: the
     * reminder and boot receivers reach them without an Activity. Every change
     * to the settings reschedules the reminders.
     */
    val slavaStore: SlavaStore by lazy {
        SlavaStore(this).also { store ->
            store.onChange = {
                slavaReminders.update()
                // The wide widget counts down to the user's slava.
                widgetSync.refresh()
            }
        }
    }

    val slavaReminders: SlavaReminders by lazy {
        // The repository is handed over as a provider so a reschedule that
        // needs no table line never builds it.
        SlavaReminders(this, slavaStore, { repository })
    }

    /**
     * The user's name day and friends' name days (Russian only), built like the
     * slava above, and their reminders. Every change reschedules them.
     */
    val nameDayStore: NameDayStore by lazy {
        NameDayStore(this).also { store -> store.onChange = { nameDayReminders.update() } }
    }

    val nameDayReminders: NameDayReminders by lazy { NameDayReminders(this, nameDayStore) }

    /** The bundled name-day catalog (imeniny_ru.json), read once. */
    val nameDayCatalog: NameDayCatalog get() = NameDayCatalog.shared(this)

    /**
     * Writes the home-screen widgets' snapshot (widget/WidgetSync.kt). The
     * repository is handed over as a provider, as for the reminders.
     */
    val widgetSync: WidgetSync by lazy { WidgetSync(this, slavaStore, { repository }) }

    /**
     * The repository is process-scoped — that is the point of it — so nothing
     * tied to the Activity's lifetime can free what it holds, and a process that
     * the low-memory killer would otherwise discard keeps a hundred-ish MB of
     * decoded years and text pools resident until the user force-quits.
     *
     * Everything dropped here is re-readable from assets or the disk cache, so
     * the cost is a re-decode, not a re-download. Which levels count is decided
     * by [shouldReleaseCalendarCache] — see it for why the levels cannot simply
     * be compared with `>=`.
     */
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (shouldReleaseCalendarCache(level) && repositoryLazy.isInitialized()) {
            repositoryLazy.value.releaseMemory()
        }
    }
}

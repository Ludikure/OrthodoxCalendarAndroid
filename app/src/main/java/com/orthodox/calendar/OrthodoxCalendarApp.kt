package com.orthodox.calendar

import android.app.Application
import com.orthodox.calendar.app.AppUpdateGate
import com.orthodox.calendar.data.repository.CalendarRepository

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
    val repository: CalendarRepository by lazy { CalendarRepository(this) }

    /**
     * The update gate lives here for the same reason.
     *
     * Held in the composition it was rebuilt with the Activity, so `mustUpdate`
     * fell back to false on every rotation: a user the server had blocked could
     * turn the phone and be inside the app while /api/config was refetched.
     * Process-scoped, the gate is asked once and its answer sticks.
     */
    val updateGate: AppUpdateGate by lazy { AppUpdateGate() }
}

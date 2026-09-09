package com.orthodox.calendar

import android.app.Application
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

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: OrthodoxCalendarApp
            private set
    }
}

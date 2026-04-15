package com.orthodox.calendar

import android.app.Application

class OrthodoxCalendarApp : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: OrthodoxCalendarApp
            private set
    }
}

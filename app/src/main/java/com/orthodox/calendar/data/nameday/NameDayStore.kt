package com.orthodox.calendar.data.nameday

import android.content.Context
import android.content.SharedPreferences
import com.orthodox.calendar.data.model.CalendarDay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import java.time.LocalDate

/**
 * The user's name day (именины), their friends', and how to be reminded.
 *
 * Russian only: the screens show none of this in other languages and
 * `NameDayReminders` schedules nothing there — just as slavas are Serbian only,
 * so the two never meet on one screen. Kept on the device in SharedPreferences
 * as one JSON value, read synchronously once, like [com.orthodox.calendar.data.slava.SlavaStore].
 *
 * Mirror of `OrthodoxCalendar/App/NameDayStore.swift`.
 */
class NameDayStore(private val prefs: SharedPreferences) {
    constructor(context: Context) :
        this(context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE))

    private val _settings = MutableStateFlow(load())
    val settings: StateFlow<NameDaySettings> = _settings.asStateFlow()

    /** Called after every change; the app reschedules reminders from it. */
    @Volatile var onChange: (() -> Unit)? = null

    /** Applies [transform] and persists the result; a no-op change stays silent. */
    fun update(transform: (NameDaySettings) -> NameDaySettings) {
        synchronized(this) {
            val old = _settings.value
            val new = transform(old)
            if (new == old) return
            _settings.value = new
            prefs.edit().putString(KEY_SETTINGS, encode(new)).apply()
        }
        onChange?.invoke()
    }

    fun mark(day: CalendarDay): NameDayMark? = _settings.value.mark(day)

    fun countdown(today: LocalDate = LocalDate.now()): NameDayCountdown? = _settings.value.countdown(today)

    private fun load(): NameDaySettings =
        prefs.getString(KEY_SETTINGS, null)?.let(::decode) ?: NameDaySettings()

    companion object {
        /** How far ahead the month banner starts counting down. */
        const val BANNER_DAYS = 30

        private const val PREFS_NAME = "imeniny"
        private const val KEY_SETTINGS = "settings"

        private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

        fun encode(settings: NameDaySettings): String = json.encodeToString(NameDaySettings.serializer(), settings)

        /** Null for a value this version cannot read, which then starts fresh. */
        fun decode(text: String): NameDaySettings? =
            runCatching { json.decodeFromString(NameDaySettings.serializer(), text) }.getOrNull()
    }
}

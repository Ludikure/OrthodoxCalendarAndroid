package com.orthodox.calendar.data.slava

import android.content.Context
import android.content.SharedPreferences
import com.orthodox.calendar.data.model.CalendarDay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import java.time.LocalDate

/**
 * The user's krsna slava, their friends' slavas, and how to be reminded.
 *
 * Serbian only: slava is a Serbian custom, so the screens show none of this in
 * other languages and `SlavaReminders` schedules nothing there. Kept on the
 * device in SharedPreferences as one JSON value, read synchronously once so the
 * first frame already knows the slava (a DataStore flow would draw the month
 * without its gold mark and then flash it in).
 *
 * Mirror of `OrthodoxCalendar/App/SlavaStore.swift`.
 */
class SlavaStore(private val prefs: SharedPreferences) {
    constructor(context: Context) :
        this(context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE))

    private val _settings = MutableStateFlow(load())
    val settings: StateFlow<SlavaSettings> = _settings.asStateFlow()

    /** Called after every change; the app reschedules reminders from it. */
    @Volatile var onChange: (() -> Unit)? = null

    /** Applies [transform] and persists the result; a no-op change stays silent. */
    fun update(transform: (SlavaSettings) -> SlavaSettings) {
        val old: SlavaSettings
        val new: SlavaSettings
        synchronized(this) {
            old = _settings.value
            new = transform(old)
            if (new == old) return
            _settings.value = new
            prefs.edit().putString(KEY_SETTINGS, encode(new)).apply()
        }
        onChange?.invoke()
    }

    fun mark(day: CalendarDay): SlavaMark? = _settings.value.mark(day)

    fun countdown(today: LocalDate = LocalDate.now()): SlavaCountdown? =
        _settings.value.countdown(today)

    private fun load(): SlavaSettings =
        prefs.getString(KEY_SETTINGS, null)?.let(::decode) ?: SlavaSettings()

    companion object {
        /** How far ahead the month banner starts counting down. */
        const val BANNER_DAYS = 30

        private const val PREFS_NAME = "slava"
        private const val KEY_SETTINGS = "settings"

        private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

        fun encode(settings: SlavaSettings): String = json.encodeToString(SlavaSettings.serializer(), settings)

        /** Null for a value this version cannot read, which then starts fresh. */
        fun decode(text: String): SlavaSettings? =
            runCatching { json.decodeFromString(SlavaSettings.serializer(), text) }.getOrNull()
    }
}

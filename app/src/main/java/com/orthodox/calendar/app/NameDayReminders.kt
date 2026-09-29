package com.orthodox.calendar.app

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import com.orthodox.calendar.data.model.AppLanguage
import com.orthodox.calendar.data.nameday.NameDayChoice
import com.orthodox.calendar.data.nameday.NameDaySettings
import com.orthodox.calendar.data.nameday.NameDayStore
import com.orthodox.calendar.data.nameday.NameDayText
import com.orthodox.calendar.data.preferences.AppPreferences
import com.orthodox.calendar.data.preferences.firstOrDefault
import com.orthodox.calendar.ui.util.toIsoDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Local notifications for name days: the morning of the user's own, and on the
 * day (optionally also the day before) of each friend's.
 *
 * Same scheduling as [SlavaReminders], and the same receivers: rebuilt from
 * scratch whenever the settings or the language change, when the app comes to
 * the foreground and after a reboot, for the next two occurrences of each name
 * day; inexact alarms. Only the Russian calendar shows name days, so any other
 * language clears them.
 *
 * Mirror of `OrthodoxCalendar/App/NameDayReminders.swift`.
 */
class NameDayReminders(
    private val context: Context,
    private val store: NameDayStore,
    private val language: suspend () -> AppLanguage = {
        AppPreferences(context).languageFlow.firstOrDefault(AppLanguage.SR)
    }
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mutex = Mutex()
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Queues a reschedule behind any still running. */
    fun update(): Job = scope.launch { reschedule() }

    suspend fun reschedule() = mutex.withLock {
        cancelScheduled()
        if (language() != AppLanguage.RU) return@withLock
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return@withLock

        val reminders = plan(store.settings.value, LocalDateTime.now())
            .sortedBy { it.fireAt }
            .take(MAX_ALARMS)
        val alarms = context.getSystemService(AlarmManager::class.java) ?: return@withLock
        val zone = ZoneId.systemDefault()
        reminders.forEachIndexed { code, r ->
            val millis = r.fireAt.atZone(zone).toInstant().toEpochMilli()
            runCatching {
                alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pendingIntent(code, r))
            }
        }
        prefs.edit().putInt(KEY_COUNT, reminders.size).apply()
    }

    private fun cancelScheduled() {
        val alarms = context.getSystemService(AlarmManager::class.java) ?: return
        val count = prefs.getInt(KEY_COUNT, 0)
        for (code in 0 until count) {
            val intent = Intent(context, SlavaReminderReceiver::class.java).setAction(ACTION_FIRE)
            PendingIntent.getBroadcast(
                context, code, intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )?.let { alarms.cancel(it); it.cancel() }
        }
        prefs.edit().putInt(KEY_COUNT, 0).apply()
    }

    // Its own action, so these request codes never collide with the slava
    // alarms' (a PendingIntent is identified by action and code, not extras).
    private fun pendingIntent(code: Int, r: SlavaReminders.Reminder): PendingIntent {
        val intent = Intent(context, SlavaReminderReceiver::class.java)
            .setAction(ACTION_FIRE)
            .putExtra(SlavaReminders.EXTRA_ID, r.key.hashCode())
            .putExtra(SlavaReminders.EXTRA_TITLE, r.title)
            .putExtra(SlavaReminders.EXTRA_BODY, r.body)
        return PendingIntent.getBroadcast(
            context, code, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        private const val PREFS_NAME = "imeniny_reminders"
        private const val KEY_COUNT = "scheduledCount"
        private const val MAX_ALARMS = 60

        const val CHANNEL_ID = "imeniny_reminders"
        internal const val ACTION_FIRE = "com.orthodox.calendar.NAME_DAY_REMINDER"

        /** Every reminder [settings] asks for after [now], worded as iOS words them. */
        fun plan(settings: NameDaySettings, now: LocalDateTime): List<SlavaReminders.Reminder> {
            val out = mutableListOf<SlavaReminders.Reminder>()
            val minutes = settings.reminderMinutes.toLong()
            fun fire(day: LocalDate, daysBefore: Long) =
                day.minusDays(daysBefore).atStartOfDay().plusMinutes(minutes)

            val mine = settings.mine
            if (mine != null && settings.remindOnDay) {
                for (date in nextTwo(mine, now.toLocalDate())) {
                    val at = fire(date, 0)
                    if (!at.isAfter(now)) continue
                    out += SlavaReminders.Reminder(
                        "imeniny.mine.day.${date.toIsoDate()}", at, "С днём ангела!",
                        "Сегодня ваши именины — память: ${mine.title}."
                    )
                }
            }

            for (friend in settings.friends) {
                for (date in nextTwo(friend.nameDay, now.toLocalDate())) {
                    val who = friend.displayName
                    val onDay = fire(date, 0)
                    if (settings.remindFriends && onDay.isAfter(now)) {
                        out += SlavaReminders.Reminder(
                            "imeniny.friend.${friend.id}.day.${date.toIsoDate()}", onDay,
                            "Сегодня именины: $who",
                            "${friend.nameDay.churchName} — не забудьте поздравить."
                        )
                    }
                    val eve = fire(date, 1)
                    if (settings.remindFriendsDayBefore && eve.isAfter(now)) {
                        out += SlavaReminders.Reminder(
                            "imeniny.friend.${friend.id}.eve.${date.toIsoDate()}", eve,
                            "Завтра именины: $who",
                            "${friend.nameDay.churchName}, ${NameDayText.longDate(date)}."
                        )
                    }
                }
            }
            return out
        }

        private fun nextTwo(nameDay: NameDayChoice, from: LocalDate): List<LocalDate> {
            val first = nameDay.nextOccurrence(from) ?: return emptyList()
            return listOfNotNull(first, nameDay.nextOccurrence(first.plusDays(1)))
        }

        /** A channel of its own, named in Russian: the slava one is named in Serbian. */
        fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val manager = context.getSystemService(NotificationManager::class.java) ?: return
            if (manager.getNotificationChannel(CHANNEL_ID) != null) return
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Именины", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
    }
}

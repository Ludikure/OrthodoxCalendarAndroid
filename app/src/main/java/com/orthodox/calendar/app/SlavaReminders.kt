package com.orthodox.calendar.app

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.orthodox.calendar.MainActivity
import com.orthodox.calendar.OrthodoxCalendarApp
import com.orthodox.calendar.R
import com.orthodox.calendar.data.model.AppLanguage
import com.orthodox.calendar.data.preferences.AppPreferences
import com.orthodox.calendar.data.preferences.firstOrDefault
import com.orthodox.calendar.data.repository.YearSource
import com.orthodox.calendar.data.slava.SlavaDay
import com.orthodox.calendar.data.slava.SlavaSettings
import com.orthodox.calendar.data.slava.SlavaStore
import com.orthodox.calendar.data.slava.SlavaText
import com.orthodox.calendar.ui.util.toIsoDate
import kotlinx.coroutines.CancellationException
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
 * Local notifications for slavas: a week before and on the morning of the
 * user's own, and the day before each friend's.
 *
 * Rescheduled from scratch whenever the settings or the language change, when
 * the app comes to the foreground and after a reboot (alarms do not survive
 * one), for the next two occurrences of each slava — so a reminder still fires
 * a year on if the app isn't opened. Only the Serbian calendar shows slavas, so
 * any other language clears them.
 *
 * Alarms are inexact (`setAndAllowWhileIdle`): a morning reminder a few minutes
 * late is fine, and exact alarms need a permission the Play Store polices.
 *
 * Mirror of `OrthodoxCalendar/App/SlavaReminders.swift`.
 */
class SlavaReminders(
    private val context: Context,
    private val store: SlavaStore,
    private val years: () -> YearSource,
    private val language: suspend () -> AppLanguage = {
        AppPreferences(context).languageFlow.firstOrDefault(AppLanguage.SR)
    }
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    /** Reschedules run one at a time, so two quick settings changes can't
     *  interleave one's cancellations with the other's alarms. */
    private val mutex = Mutex()
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Queues a reschedule behind any still running. */
    fun update(): Job = scope.launch { reschedule() }

    /** One reminder as it will be posted. */
    data class Reminder(val key: String, val fireAt: LocalDateTime, val title: String, val body: String)

    suspend fun reschedule() = mutex.withLock {
        cancelScheduled()
        if (language() != AppLanguage.SR) return@withLock
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return@withLock

        val reminders = plan(store.settings.value, LocalDateTime.now()) { date -> tableLine(date) }
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

    private fun pendingIntent(code: Int, r: Reminder): PendingIntent {
        val intent = Intent(context, SlavaReminderReceiver::class.java)
            .setAction(ACTION_FIRE)
            .putExtra(EXTRA_ID, r.key.hashCode())
            .putExtra(EXTRA_TITLE, r.title)
            .putExtra(EXTRA_BODY, r.body)
        return PendingIntent.getBroadcast(
            context, code, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * The fasting rule for the slava table, from a year already on the device
     * (never a download just to word a reminder).
     */
    private suspend fun tableLine(date: LocalDate): String? = try {
        val file = years().load("sr", date.year, allowNetwork = false)
        file.days[date.toIsoDate().drop(5)]?.let { SlavaText.table(it.fasting.type) }
    } catch (c: CancellationException) {
        throw c
    } catch (e: Exception) {
        null
    }

    companion object {
        private const val PREFS_NAME = "slava_reminders"
        private const val KEY_COUNT = "scheduledCount"
        /** iOS keeps at most 64 pending notifications and schedules 60; kept alike. */
        private const val MAX_ALARMS = 60

        const val CHANNEL_ID = "slava_reminders"
        internal const val ACTION_FIRE = "com.orthodox.calendar.SLAVA_REMINDER"
        internal const val EXTRA_ID = "id"
        internal const val EXTRA_TITLE = "title"
        internal const val EXTRA_BODY = "body"

        /**
         * Every reminder [settings] asks for after [now], worded as iOS words
         * them. [table] gives the slava table line for a date, or null when that
         * year is not on the device.
         */
        suspend fun plan(
            settings: SlavaSettings,
            now: LocalDateTime,
            table: suspend (LocalDate) -> String?
        ): List<Reminder> {
            val out = mutableListOf<Reminder>()
            val minutes = settings.reminderMinutes.toLong()
            fun fire(day: LocalDate, daysBefore: Long) =
                day.minusDays(daysBefore).atStartOfDay().plusMinutes(minutes)

            settings.mine?.let { mine ->
                for (date in nextTwo(mine, now.toLocalDate())) {
                    val line = table(date)
                    val week = fire(date, 7)
                    if (settings.remindWeekBefore && week.isAfter(now)) {
                        var body = "Жито, колач и свећа. Позовите свештеника за освећење водице."
                        if (line != null) body += " $line"
                        out += Reminder("mine.week.${date.toIsoDate()}", week, "${mine.name} за 7 дана", body)
                    }
                    val onDay = fire(date, 0)
                    if (settings.remindOnDay && onDay.isAfter(now)) {
                        var body = "Данас је ${mine.name}."
                        if (line != null) body += " $line"
                        out += Reminder("mine.day.${date.toIsoDate()}", onDay, "Срећна слава!", body)
                    }
                }
            }

            if (settings.remindFriends) {
                for (friend in settings.friends) {
                    for (date in nextTwo(friend.slava, now.toLocalDate())) {
                        val at = fire(date, 1)
                        if (!at.isAfter(now)) continue
                        out += Reminder(
                            "friend.${friend.id}.${date.toIsoDate()}", at,
                            "Сутра: слава — ${friend.person}",
                            "${friend.slava.name}, ${SlavaText.longDate(date)}."
                        )
                    }
                }
            }
            return out
        }

        private fun nextTwo(slava: SlavaDay, from: LocalDate): List<LocalDate> {
            val first = slava.nextOccurrence(from) ?: return emptyList()
            return listOfNotNull(first, slava.nextOccurrence(first.plusDays(1)))
        }

        fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val manager = context.getSystemService(NotificationManager::class.java) ?: return
            if (manager.getNotificationChannel(CHANNEL_ID) != null) return
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Подсетници за славу", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
    }
}

/** Posts a slava reminder when its alarm fires. */
class SlavaReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != SlavaReminders.ACTION_FIRE) return
        val title = intent.getStringExtra(SlavaReminders.EXTRA_TITLE) ?: return
        val body = intent.getStringExtra(SlavaReminders.EXTRA_BODY).orEmpty()
        val id = intent.getIntExtra(SlavaReminders.EXTRA_ID, 0)

        SlavaReminders.ensureChannel(context)
        val open = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, SlavaReminders.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_slava)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        // The permission can be withdrawn between scheduling and firing.
        runCatching { NotificationManagerCompat.from(context).notify(id, notification) }
    }
}

/**
 * Alarms do not survive a reboot, an app update or a clock change; each of those
 * schedules the reminders again.
 */
class SlavaRescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_TIME_CHANGED -> Unit
            else -> return
        }
        val app = context.applicationContext as? OrthodoxCalendarApp ?: return
        val pending = goAsync()
        app.slavaReminders.update().invokeOnCompletion { pending.finish() }
    }
}

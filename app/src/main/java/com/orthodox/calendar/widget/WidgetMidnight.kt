package com.orthodox.calendar.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.orthodox.calendar.ui.util.millisUntilNextMidnight
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDateTime

/**
 * Turns the widget's page at local midnight without the app being opened: an
 * inexact alarm (`setAndAllowWhileIdle`, no exact-alarm permission) at the next
 * midnight redraws every widget, which picks the new day from the snapshot and
 * arms the following midnight. A few minutes late in Doze is fine.
 *
 * `updatePeriodMillis` in the provider XML is the fallback if an alarm is lost
 * (force-stop clears them); clock and time-zone changes redraw at once.
 */
object WidgetMidnight {
    internal const val ACTION_MIDNIGHT = "com.orthodox.calendar.WIDGET_MIDNIGHT"

    fun schedule(context: Context) {
        val alarms = context.getSystemService(AlarmManager::class.java) ?: return
        val at = System.currentTimeMillis() + millisUntilNextMidnight(LocalDateTime.now())
        runCatching { alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pendingIntent(context)) }
    }

    /** Called when a widget kind loses its last instance; keeps the alarm while another remains. */
    fun cancelIfUnused(context: Context) {
        val app = context.applicationContext
        val pending = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        pending.launch {
            if (TodayWidgetUpdater.hasWidgets(app)) return@launch
            app.getSystemService(AlarmManager::class.java)?.cancel(pendingIntent(app))
        }
    }

    private fun pendingIntent(context: Context): PendingIntent =
        PendingIntent.getBroadcast(
            context, 0,
            Intent(context, WidgetMidnightReceiver::class.java).setAction(ACTION_MIDNIGHT),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
}

/** Redraws the widgets at midnight and when the clock or time zone changes. */
class WidgetMidnightReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            WidgetMidnight.ACTION_MIDNIGHT,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> Unit
            else -> return
        }
        val app = context.applicationContext
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                if (TodayWidgetUpdater.hasWidgets(app)) {
                    TodayWidgetUpdater.updateAll(app)
                    // Redrawing re-arms it too; arming here as well keeps the
                    // chain going if the launcher skips an update.
                    WidgetMidnight.schedule(app)
                }
            } finally {
                pending.finish()
            }
        }
    }
}

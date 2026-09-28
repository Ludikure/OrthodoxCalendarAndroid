package com.orthodox.calendar.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontFamily
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.orthodox.calendar.MainActivity
import com.orthodox.calendar.data.slava.SlavaText
import com.orthodox.calendar.ui.util.FastingStyle
import com.orthodox.calendar.ui.util.fastingStyle
import com.orthodox.calendar.ui.util.toIsoDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate
import java.util.Locale

/**
 * The home-screen widget: today's weekday and date, primary commemoration,
 * fasting badge and a ✦ on great feasts; at the wide size also the other
 * commemorations and, in Serbian, the slava countdown.
 *
 * One widget with two layouts, chosen by the size the launcher gives it, and
 * offered twice in the picker ([TodayWidgetReceiver] at 2×2,
 * [TodayWideWidgetReceiver] at 4×2) the way iOS offers its small and medium
 * families. Everything shown comes from the app's [WidgetSnapshot]; a snapshot
 * that does not reach today shows "open the app" rather than another day.
 *
 * Mirror of `OrthodoxCalendarWidget/OrthodoxCalendarWidget.swift`.
 */
class TodayWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(setOf(SMALL, WIDE))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // Every render re-arms the next midnight, so the widget turns the page
        // even if the app is never opened (alarms do not survive a reboot; the
        // launcher's own update after one lands here too).
        WidgetMidnight.schedule(context)
        provideContent {
            // The session lives on for a while after it renders, and update()
            // does not restart provideGlance while it does: a new snapshot or
            // a new day reaches a running session through this counter.
            val revision by TodayWidgetUpdater.revision.collectAsState()
            val state = remember(revision) { WidgetState.load(context) }
            WidgetContent(state)
        }
    }

    companion object {
        val SMALL = DpSize(110.dp, 110.dp)
        val WIDE = DpSize(250.dp, 110.dp)
    }
}

/** The picker's 2×2 entry. */
class TodayWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodayWidget()

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        WidgetMidnight.cancelIfUnused(context)
    }
}

/** The picker's 4×2 entry: the same widget, placed wide. */
class TodayWideWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodayWidget()

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        WidgetMidnight.cancelIfUnused(context)
    }
}

/** Redraws every placed widget, running sessions included. */
object TodayWidgetUpdater {
    private val _revision = MutableStateFlow(0)
    val revision: StateFlow<Int> = _revision.asStateFlow()

    suspend fun updateAll(context: Context) {
        _revision.value++
        runCatching { TodayWidget().updateAll(context.applicationContext) }
    }

    /** Whether any widget of ours is on a home screen. */
    suspend fun hasWidgets(context: Context): Boolean = runCatching {
        GlanceAppWidgetManager(context).getGlanceIds(TodayWidget::class.java).isNotEmpty()
    }.getOrDefault(false)
}

/** What one render shows: the snapshot's entry for today, if it has one. */
internal data class WidgetState(val language: String, val day: WidgetSnapshot.Day?) {
    companion object {
        fun load(context: Context, today: LocalDate = LocalDate.now()): WidgetState {
            val snapshot = WidgetSnapshot.read(context)
            return WidgetState(
                language = snapshot?.language ?: WidgetStrings.deviceLanguage(),
                day = snapshot?.day(today.toIsoDate())
            )
        }
    }
}

/** The widget's own few strings, by `AppLanguage.code`. */
internal class WidgetStrings(private val language: String) {
    val openApp: String
        get() = when (language) {
            "sr" -> "Отворите апликацију"
            "ru" -> "Откройте приложение"
            else -> "Open the app"
        }

    val greatFeast: String
        get() = when (language) {
            "sr" -> "Велики празник"
            "ru" -> "Великий праздник"
            else -> "Great Feast"
        }

    companion object {
        /** Before the app has written a snapshot: the device's language, Serbian otherwise. */
        fun deviceLanguage(): String = when (Locale.getDefault().language) {
            "ru" -> "ru"
            "en" -> "en"
            else -> "sr"
        }
    }
}

/**
 * The app's palette as day/night pairs, which is how a widget follows the
 * system theme. Values in step with `ui/theme/AppColors.kt`; the widget cannot
 * read those, which switch on the app's own theme setting inside Compose.
 */
private object WidgetColors {
    private fun pair(day: Long, night: Long) = ColorProvider(day = Color(day), night = Color(night))

    val background = pair(0xFFF5F3EE, 0xFF1C1A17)      // warmBg
    val crimson = ColorProvider(Color(0xFFC94040))
    val mutedText = pair(0xFF8C7E6A, 0xFF998C7A)
    val bodyText = pair(0xFF5C5040, 0xFFBFB3A1)
    val darkText = pair(0xFF2C2418, 0xFFE6DED1)
    val bannerTitle = pair(0xFF7A1F1A, 0xFFDCC089)
    val goldAccent = pair(0xFFD4C5A9, 0xFFD1BD94)
    val slavaGold = pair(0xFF8A6A12, 0xFFD9B95A)

    /** Icon, ink and tint of the calendar row's fasting badge. */
    fun fasting(type: String): Triple<String, ColorProvider, ColorProvider> =
        when (fastingStyle(type)) {
            FastingStyle.STRICT -> Triple(
                if (type.lowercase(Locale.ROOT) == "dryeating") "🍞" else "🚫",
                ColorProvider(Color(0xFF7B2D8E)), pair(0xFFF3E8F8, 0xFF401F4D)
            )
            FastingStyle.WATER -> Triple(
                "💧", ColorProvider(Color(0xFF2E7D9B)), pair(0xFFE4F2F8, 0xFF1A3847)
            )
            FastingStyle.OIL -> Triple(
                "🫒", ColorProvider(Color(0xFF8B7B2D)), pair(0xFFFFF8E1, 0xFF40381A)
            )
            FastingStyle.FISH -> Triple(
                "🐟", ColorProvider(Color(0xFF2D6B4F)), pair(0xFFE8F5EC, 0xFF1A3326)
            )
            FastingStyle.FREE -> Triple(
                "✓", ColorProvider(Color(0xFF4A7C3F)), pair(0xFFEDF8EA, 0xFF24381F)
            )
        }
}

/** Opens the app on today, with today's detail (`MainActivity.ACTION_OPEN_TODAY`). */
private fun openTodayIntent(context: Context): Intent =
    Intent(context, MainActivity::class.java)
        .setAction(MainActivity.ACTION_OPEN_TODAY)
        .addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
        )

@Composable
private fun WidgetContent(state: WidgetState) {
    val context = LocalContext.current
    val strings = WidgetStrings(state.language)
    val wide = LocalSize.current.width >= TodayWidget.WIDE.width
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(WidgetColors.background)
            .cornerRadius(20.dp)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .clickable(actionStartActivity(openTodayIntent(context))),
        contentAlignment = if (state.day == null) Alignment.Center else Alignment.TopStart
    ) {
        val day = state.day
        when {
            day == null -> Placeholder(strings)
            wide -> WideDay(day, strings)
            else -> SmallDay(day)
        }
    }
}

@Composable
private fun Placeholder(strings: WidgetStrings) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "☦",
            style = TextStyle(
                color = WidgetColors.crimson, fontSize = 28.sp, fontFamily = FontFamily.Serif
            )
        )
        Text(
            strings.openApp,
            style = TextStyle(
                color = WidgetColors.bodyText, fontSize = 14.sp, fontFamily = FontFamily.Serif,
                textAlign = TextAlign.Center
            )
        )
    }
}

/** Weekday over the date; the ✦ on a great feast. */
@Composable
private fun DateHeader(day: WidgetSnapshot.Day) {
    Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                day.weekday.uppercase(),
                style = TextStyle(color = WidgetColors.crimson, fontSize = 11.sp, fontWeight = FontWeight.Bold),
                maxLines = 1
            )
            Text(
                day.dateLabel,
                style = TextStyle(color = WidgetColors.mutedText, fontSize = 12.sp),
                maxLines = 1
            )
        }
        if (day.isGreatFeast) {
            Text(
                "✦",
                style = TextStyle(color = WidgetColors.crimson, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            )
        }
    }
}

/** The calendar row's fasting badge: icon and short label in a tinted capsule. */
@Composable
private fun FastingBadge(day: WidgetSnapshot.Day) {
    val (icon, ink, tint) = WidgetColors.fasting(day.fastingType)
    val text = day.fastingAbbrev.ifEmpty { day.fastingLabel }
    Row(
        modifier = GlanceModifier
            .background(tint)
            .cornerRadius(10.dp)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, style = TextStyle(fontSize = 10.sp))
        Spacer(GlanceModifier.width(3.dp))
        Text(text, style = TextStyle(color = ink, fontSize = 10.sp, fontWeight = FontWeight.Bold), maxLines = 1)
    }
}

@Composable
private fun PrimaryName(day: WidgetSnapshot.Day, lines: Int, size: TextUnit = 15.sp) {
    Text(
        day.primary,
        style = TextStyle(
            color = if (day.isGreatFeast) WidgetColors.bannerTitle else WidgetColors.darkText,
            fontSize = size,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Serif
        ),
        maxLines = lines
    )
}

@Composable
private fun SmallDay(day: WidgetSnapshot.Day) {
    Column(modifier = GlanceModifier.fillMaxSize()) {
        DateHeader(day)
        Spacer(GlanceModifier.defaultWeight())
        PrimaryName(day, lines = 3)
        Spacer(GlanceModifier.defaultWeight())
        FastingBadge(day)
    }
}

@Composable
private fun WideDay(day: WidgetSnapshot.Day, strings: WidgetStrings) {
    Row(modifier = GlanceModifier.fillMaxSize()) {
        Column(modifier = GlanceModifier.width(112.dp).fillMaxHeight()) {
            DateHeader(day)
            Spacer(GlanceModifier.defaultWeight())
            FastingBadge(day)
            Spacer(GlanceModifier.height(4.dp))
            Text(
                day.fastingLabel,
                style = TextStyle(color = WidgetColors.mutedText, fontSize = 11.sp),
                maxLines = 2
            )
        }
        Spacer(GlanceModifier.width(12.dp))
        Box(modifier = GlanceModifier.width(1.dp).fillMaxHeight().background(WidgetColors.goldAccent)) {}
        Spacer(GlanceModifier.width(12.dp))
        Column(modifier = GlanceModifier.defaultWeight().fillMaxHeight()) {
            if (day.isGreatFeast) {
                Text(
                    "✦ ${strings.greatFeast.uppercase()}",
                    style = TextStyle(color = WidgetColors.crimson, fontSize = 9.sp, fontWeight = FontWeight.Bold),
                    maxLines = 1
                )
                Spacer(GlanceModifier.height(3.dp))
            }
            PrimaryName(day, lines = 2)
            if (day.secondary.isNotEmpty()) {
                Spacer(GlanceModifier.height(3.dp))
                Text(
                    day.secondary.joinToString("; "),
                    style = TextStyle(color = WidgetColors.mutedText, fontSize = 12.sp),
                    maxLines = if (day.slava == null) 3 else 2
                )
            }
            val slava = day.slava
            if (slava != null) {
                Spacer(GlanceModifier.defaultWeight())
                // iOS shrinks this line to fit; RemoteViews cannot, so a long
                // slava name ("Покров Пресвете Богородице") steps down a size
                // and may wrap rather than cut off the countdown.
                val line = "🕯 ${slava.name} · ${SlavaText.countdownLabel(slava.daysUntil)}"
                Text(
                    line,
                    style = TextStyle(
                        color = WidgetColors.slavaGold,
                        fontSize = if (line.length > 32) 11.sp else 12.sp,
                        fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif
                    ),
                    maxLines = 2
                )
            }
        }
    }
}

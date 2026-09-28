package com.orthodox.calendar.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orthodox.calendar.data.model.AppLanguage
import com.orthodox.calendar.data.model.FastingPeriodInfo
import com.orthodox.calendar.data.model.FastingPeriods
import com.orthodox.calendar.data.model.LocalizationBundle
import com.orthodox.calendar.data.slava.SlavaCountdown
import com.orthodox.calendar.data.slava.SlavaText
import com.orthodox.calendar.ui.theme.AppColors

/**
 * Banner under the month bar (list & grid views). Its first row is the fasting
 * season — name, date range and, for today, "Day X of Y" — and its second, in
 * the 30 days before the user's slava, a countdown to it. Either row can be
 * absent; with both, they share one card under a thin rule, so a slava inside a
 * fast (Никољдан always is) never hides the fast and costs the list one short
 * row instead of a second banner.
 *
 * Mirror of `OrthodoxCalendar/Views/SeasonBanner.swift`.
 */
@Composable
fun SeasonBanner(
    period: FastingPeriodInfo?,
    localization: LocalizationBundle,
    language: AppLanguage,
    modifier: Modifier = Modifier,
    // "Day X of Y" is only meaningful for today. As a month overview (browsing a
    // season that isn't currently active) the focal day isn't today, so its index
    // would be an arbitrary position in the run — hide it, show the range alone.
    showsDayIndex: Boolean = true,
    slava: SlavaCountdown? = null,
    onSlavaTap: () -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(AppColors.bannerBg)
    ) {
        if (period != null) {
            PeriodRow(period, localization, language, showsDayIndex)
        }
        if (period != null && slava != null) {
            Spacer(
                modifier = Modifier
                    .padding(horizontal = 14.dp)
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(AppColors.bannerDivider)
            )
        }
        if (slava != null) {
            SlavaRow(slava, onSlavaTap)
        }
    }
}

@Composable
private fun PeriodRow(
    period: FastingPeriodInfo,
    localization: LocalizationBundle,
    language: AppLanguage,
    showsDayIndex: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(text = "⛪", fontSize = 16.sp)
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = period.displayName,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.bannerTitle
            )
            // Date range only when the run is fully known (a season truncated at
            // the data boundary would mislead); the "Day X of Y" suffix only when
            // it refers to today.
            if (period.complete) {
                // After a day number Russian takes the genitive: "15 мар – 1 мая".
                val range = FastingPeriods.dateRange(period, localization.ui.monthsGenitive ?: localization.ui.months)
                Text(
                    text = if (showsDayIndex) {
                        "$range  ·  ${FastingPeriods.dayLabel(language, period.dayIndex, period.total)}"
                    } else {
                        range
                    },
                    fontSize = 12.sp,
                    color = AppColors.bannerSubtext
                )
            }
        }
    }
}

@Composable
private fun SlavaRow(slava: SlavaCountdown, onTap: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClickLabel = "Отвара дан славе", onClick = onTap)
            .padding(horizontal = 14.dp, vertical = 9.dp)
    ) {
        Text(text = "🕯", fontSize = 16.sp)
        Spacer(modifier = Modifier.width(10.dp))
        SlavaTitle(slava, Modifier.weight(1f))
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = SlavaText.countdownLabel(slava.days),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = AppColors.slavaGold,
            maxLines = 1,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(AppColors.cardBg)
                .border(BorderStroke(1.dp, AppColors.gold), RoundedCornerShape(50))
                .padding(horizontal = 9.dp, vertical = 4.dp)
        )
    }
}

/**
 * "Никољдан — ваша слава" when it fits; a long name ("Покров Пресвете
 * Богородице") keeps the name and drops the suffix, and one that still doesn't
 * fit wraps to two lines — iOS's `ViewThatFits` over the same three forms.
 */
@Composable
private fun SlavaTitle(slava: SlavaCountdown, modifier: Modifier) {
    val style = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Serif,
        color = AppColors.bannerTitle
    )
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    BoxWithConstraints(modifier = modifier) {
        val maxPx = with(density) { maxWidth.roundToPx() }
        val today = slava.days == 0
        val candidates = listOf(
            if (today) "Данас: ${slava.name} — ваша слава" else "${slava.name} — ваша слава",
            if (today) "Данас: ${slava.name}" else slava.name
        )
        val fits = candidates.firstOrNull {
            measurer.measure(it, style, maxLines = 1, softWrap = false).size.width <= maxPx
        }
        Text(
            text = fits ?: slava.name,
            style = style,
            maxLines = if (fits != null) 1 else 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

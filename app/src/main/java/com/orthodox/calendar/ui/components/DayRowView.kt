package com.orthodox.calendar.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orthodox.calendar.data.model.AppLanguage
import com.orthodox.calendar.data.model.CalendarDay
import com.orthodox.calendar.data.model.LocalizationBundle
import com.orthodox.calendar.ui.theme.AppColors

@Composable
fun DayRowView(
    day: CalendarDay,
    isToday: Boolean,
    localization: LocalizationBundle,
    language: AppLanguage = AppLanguage.SR,
    modifier: Modifier = Modifier
) {
    val isGreatFeast = day.isGreatFeast
    val isRed = day.primaryFeast?.importance == "great"
    val isBold = day.primaryFeast?.importance.let { it == "bold" || it == "great" }

    val dayOfWeekAbbrev = run {
        val abbrevs = localization.ui.daysOfWeek
        val idx = day.weekdayIndex
        if (idx in abbrevs.indices) abbrevs[idx].take(2) else ""
    }

    val rowBgModifier = when {
        isToday -> Modifier.background(AppColors.crimson.copy(alpha = 0.08f))
        isGreatFeast -> Modifier.background(
            Brush.horizontalGradient(
                colors = listOf(Color(0xFFFFF8F0), Color(0xFFFCEBD1))
            )
        )
        day.isSunday -> Modifier.background(AppColors.crimson.copy(alpha = 0.04f))
        else -> Modifier
    }

    Box(modifier = modifier.fillMaxWidth()) {
        // Left crimson bar for great feasts
        if (isGreatFeast) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .matchParentSize()
                    .background(AppColors.crimson)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(rowBgModifier)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Day number + weekday column
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(52.dp)
                    .padding(top = 2.dp)
            ) {
                // Day number
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(34.dp)
                ) {
                    if (isToday) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(AppColors.crimson)
                        )
                    }
                    Text(
                        text = day.gregorianDay.toString(),
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = if (isToday) Color.White
                        else if (day.isSunday) AppColors.crimson
                        else AppColors.darkText
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Weekday abbreviation
                Text(
                    text = dayOfWeekAbbrev,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp,
                    color = if (isToday || day.isSunday) AppColors.crimson else AppColors.mutedText
                )

                // Julian day
                Text(
                    text = day.julianDay.toString(),
                    fontSize = 9.sp,
                    color = AppColors.lightMuted
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Main content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                if (isGreatFeast) {
                    val greatFeastLabel = when (language) {
                        AppLanguage.SR -> "\u0412\u0435\u043B\u0438\u043A\u0438 \u043F\u0440\u0430\u0437\u043D\u0438\u043A"
                        AppLanguage.RU -> "\u0412\u0435\u043B\u0438\u043A\u0438\u0439 \u043F\u0440\u0430\u0437\u0434\u043D\u0438\u043A"
                        AppLanguage.EN, AppLanguage.EN_NC -> "GREAT FEAST"
                    }
                    Text(
                        text = "\u2726 $greatFeastLabel",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.crimson,
                        letterSpacing = 1.2.sp
                    )
                }

                // Primary feast name
                day.primaryFeast?.name?.takeIf { it.isNotEmpty() }?.let { name ->
                    Text(
                        text = name,
                        fontFamily = FontFamily.Serif,
                        fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 14.sp,
                        color = if (isRed) Color(0xFF8B1A1A) else AppColors.darkText,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Secondary feasts
                val secondaryText = (day.secondaryFeasts + day.tertiaryFeasts)
                    .map { it.name }
                    .filter { it.isNotEmpty() }
                    .joinToString("; ")

                if (secondaryText.isNotEmpty()) {
                    Text(
                        text = secondaryText,
                        fontSize = 12.sp,
                        color = AppColors.mutedText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Fasting badge
            FastingBadge(day = day)
        }
    }
}

@Composable
private fun FastingBadge(day: CalendarDay) {
    val t = day.fasting.type.lowercase()
    val (icon, color, bg) = when {
        t == "totalabstinence" -> Triple("\uD83D\uDEAB", AppColors.fastStrict, AppColors.fastStrictBg)
        t == "dryeating" -> Triple("\uD83C\uDF5E", AppColors.fastStrict, AppColors.fastStrictBg)
        t.contains("nooil") -> Triple("\uD83D\uDCA7", AppColors.fastWater, AppColors.fastWaterBg)
        t.contains("oil") -> Triple("\uD83E\uDED2", AppColors.fastOil, AppColors.fastOilBg)
        t.contains("fish") || t.contains("roe") -> Triple("\uD83D\uDC1F", AppColors.fastFish, AppColors.fastFishBg)
        else -> Triple("\u2713", AppColors.fastFree, AppColors.fastFreeBg)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(text = icon, fontSize = 10.sp)
        if (!day.fasting.abbrev.isNullOrEmpty()) {
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = day.fasting.abbrev,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = color
            )
        }
    }
}

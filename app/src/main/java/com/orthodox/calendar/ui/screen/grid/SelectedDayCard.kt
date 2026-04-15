package com.orthodox.calendar.ui.screen.grid

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orthodox.calendar.data.model.AppLanguage
import com.orthodox.calendar.data.model.CalendarDay
import com.orthodox.calendar.data.model.LocalizationBundle
import com.orthodox.calendar.ui.theme.AppColors

@Composable
fun SelectedDayCard(
    day: CalendarDay,
    localization: LocalizationBundle,
    language: AppLanguage,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isPascha = day.greatFeast == "pascha"
    val isGreat = day.isGreatFeast

    val greatFeastLabel = when (language) {
        AppLanguage.SR -> "\u0412\u0415\u041B\u0418\u041A\u0418 \u041F\u0420\u0410\u0417\u041D\u0418\u041A"
        AppLanguage.RU -> "\u0412\u0415\u041B\u0418\u041A\u0418\u0419 \u041F\u0420\u0410\u0417\u0414\u041D\u0418\u041A"
        AppLanguage.EN, AppLanguage.EN_NC -> "GREAT FEAST"
    }

    val monthName = localization.ui.months.getOrElse(day.gregorianMonth - 1) { "" }

    val cardBgModifier = when {
        isPascha -> Modifier.background(
            Brush.linearGradient(
                colors = listOf(AppColors.crimson, AppColors.crimson.copy(alpha = 0.85f))
            ),
            shape = RoundedCornerShape(14.dp)
        )
        isGreat -> Modifier.background(AppColors.cardBg, shape = RoundedCornerShape(14.dp))
        else -> Modifier.background(AppColors.warmBorder, shape = RoundedCornerShape(14.dp))
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .then(cardBgModifier)
            .clickable(onClick = onClick)
    ) {
        // Left accent bar
        Box(
            modifier = Modifier
                .width(4.dp)
                .matchParentSize()
                .background(if (isGreat) AppColors.crimson else AppColors.warmBorder)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.Start
        ) {
            if (isGreat && day.primaryFeast != null) {
                Text(
                    text = "\u2726 $greatFeastLabel",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    color = if (isPascha) AppColors.goldAccent else AppColors.crimson
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            Text(
                text = "${day.gregorianDay}. $monthName \u2014 ${day.primaryFeast?.name ?: ""}",
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = if (isPascha) Color.White else AppColors.darkText
            )

            // Fasting info
            val fastType = day.fasting.type.lowercase()
            if (fastType != "free") {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Canvas(modifier = Modifier.size(8.dp)) {
                        drawCircle(color = fastingColor(fastType))
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = day.fasting.label,
                        fontSize = 12.sp,
                        color = if (isPascha) Color.White.copy(alpha = 0.7f) else AppColors.mutedText
                    )
                }
            }
        }
    }
}

private fun fastingColor(type: String): Color {
    return when {
        type == "totalabstinence" || type == "dryeating" -> AppColors.fastStrict
        type.contains("nooil") -> AppColors.fastWater
        type.contains("oil") -> AppColors.fastOil
        type.contains("fish") || type.contains("roe") -> AppColors.fastFish
        else -> Color.Transparent
    }
}

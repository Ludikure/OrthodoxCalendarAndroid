package com.orthodox.calendar.ui.screen.grid

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orthodox.calendar.data.model.CalendarDay
import com.orthodox.calendar.ui.theme.AppColors

@Composable
fun GridDayCell(
    day: CalendarDay,
    isToday: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isGreat = day.isGreatFeast
    val isPascha = day.greatFeast == "pascha"
    val fastType = day.fasting.type.lowercase()

    val cellBg: Modifier = when {
        isPascha -> Modifier.background(
            Brush.linearGradient(
                colors = listOf(AppColors.crimson, AppColors.crimson.copy(alpha = 0.8f))
            ),
            shape = RoundedCornerShape(8.dp)
        )
        isGreat -> Modifier.background(
            AppColors.crimson.copy(alpha = 0.08f),
            shape = RoundedCornerShape(8.dp)
        )
        fastType == "totalabstinence" || fastType == "dryeating" -> Modifier.background(
            AppColors.fastStrict.copy(alpha = 0.08f),
            shape = RoundedCornerShape(8.dp)
        )
        fastType.contains("oil") -> Modifier.background(
            AppColors.fastOil.copy(alpha = 0.08f),
            shape = RoundedCornerShape(8.dp)
        )
        fastType.contains("fish") -> Modifier.background(
            AppColors.fastFish.copy(alpha = 0.08f),
            shape = RoundedCornerShape(8.dp)
        )
        else -> Modifier
    }

    val borderModifier = when {
        isSelected -> Modifier.border(2.dp, AppColors.crimson, RoundedCornerShape(8.dp))
        isToday -> Modifier.border(2.dp, AppColors.gold, RoundedCornerShape(8.dp))
        else -> Modifier
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(RoundedCornerShape(8.dp))
            .then(cellBg)
            .then(borderModifier)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Day number with optional today circle
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(30.dp)
            ) {
                if (isToday) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(AppColors.crimson)
                    )
                }
                Text(
                    text = day.gregorianDay.toString(),
                    fontFamily = FontFamily.Serif,
                    fontWeight = if (isGreat || isToday) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 14.sp,
                    color = when {
                        isPascha -> AppColors.goldAccent
                        isToday -> Color.White
                        isGreat -> AppColors.crimson
                        day.isSunday -> AppColors.crimson
                        else -> AppColors.darkText
                    }
                )
            }

            // Fasting dot
            if (fastType != "free") {
                Canvas(modifier = Modifier.size(6.dp)) {
                    drawCircle(color = fastingColor(fastType))
                }
            } else {
                Spacer(modifier = Modifier.height(6.dp))
            }
        }

        // Great feast star marker
        if (isGreat || isPascha) {
            Text(
                text = "\u2726",
                fontSize = 7.sp,
                color = if (isPascha) AppColors.goldAccent else AppColors.crimson,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(3.dp)
            )
        }

        // Fasting-season marker (Great Lent, Nativity Fast, ...)
        if (day.fastingPeriod != null && !isPascha) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 2.dp)
                    .width(14.dp)
                    .height(2.5.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(AppColors.crimson.copy(alpha = 0.7f))
            )
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

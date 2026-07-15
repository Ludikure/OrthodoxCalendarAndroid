package com.orthodox.calendar.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orthodox.calendar.data.model.AppLanguage
import com.orthodox.calendar.data.model.FastingPeriodInfo
import com.orthodox.calendar.data.model.FastingPeriods
import com.orthodox.calendar.data.model.LocalizationBundle
import com.orthodox.calendar.ui.theme.AppColors

/**
 * Season banner shown under the month bar (list & grid views): the fasting
 * period's name, its date range, and the focal day's position within it.
 */
@Composable
fun FastingPeriodBanner(
    period: FastingPeriodInfo,
    localization: LocalizationBundle,
    language: AppLanguage,
    modifier: Modifier = Modifier,
    // "Day X of Y" is only meaningful for today. As a month overview (browsing a
    // season that isn't currently active) the focal day isn't today, so its index
    // would be an arbitrary position in the run — hide it, show the range alone.
    showsDayIndex: Boolean = true
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .background(
                AppColors.bannerBg,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(text = "⛪", fontSize = 16.sp)
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(10.dp))
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
                val range = FastingPeriods.dateRange(period, localization.ui.months)
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

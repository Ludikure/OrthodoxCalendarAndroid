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
    modifier: Modifier = Modifier
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
            // Only show the date range + "Day X of Y" when the run is fully known
            // (a season truncated at the data boundary would mislead).
            if (period.complete) {
                Text(
                    text = "${FastingPeriods.dateRange(period, localization.ui.months)}  ·  " +
                        FastingPeriods.dayLabel(language, period.dayIndex, period.total),
                    fontSize = 12.sp,
                    color = AppColors.bannerSubtext
                )
            }
        }
    }
}

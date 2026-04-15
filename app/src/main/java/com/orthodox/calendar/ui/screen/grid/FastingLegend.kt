package com.orthodox.calendar.ui.screen.grid

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orthodox.calendar.data.model.AppLanguage
import com.orthodox.calendar.ui.theme.AppColors

@Composable
fun FastingLegend(
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    val oilLabel = when (language) {
        AppLanguage.SR -> "\u0423\u0459\u0435"
        AppLanguage.RU -> "\u041C\u0430\u0441\u043B\u043E"
        AppLanguage.EN, AppLanguage.EN_NC -> "Oil"
    }
    val fishLabel = when (language) {
        AppLanguage.SR -> "\u0420\u0438\u0431\u0430"
        AppLanguage.RU -> "\u0420\u044B\u0431\u0430"
        AppLanguage.EN, AppLanguage.EN_NC -> "Fish"
    }
    val strictLabel = when (language) {
        AppLanguage.SR -> "\u0421\u0442\u0440\u043E\u0433\u0438"
        AppLanguage.RU -> "\u0421\u0442\u0440\u043E\u0433\u0438\u0439"
        AppLanguage.EN, AppLanguage.EN_NC -> "Strict"
    }
    val feastLabel = when (language) {
        AppLanguage.SR -> "\u041F\u0440\u0430\u0437\u043D\u0438\u043A"
        AppLanguage.RU -> "\u041F\u0440\u0430\u0437\u0434\u043D\u0438\u043A"
        AppLanguage.EN, AppLanguage.EN_NC -> "Feast"
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        LegendItem(color = AppColors.fastOil, label = oilLabel)
        Spacer(modifier = Modifier.width(16.dp))
        LegendItem(color = AppColors.fastFish, label = fishLabel)
        Spacer(modifier = Modifier.width(16.dp))
        LegendItem(color = AppColors.fastStrict, label = strictLabel)
        Spacer(modifier = Modifier.width(16.dp))
        LegendItem(color = AppColors.crimson, label = feastLabel)
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        androidx.compose.foundation.Canvas(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
        ) {
            drawCircle(color = color)
        }
        Text(
            text = label,
            fontSize = 11.sp,
            color = AppColors.mutedText
        )
    }
}

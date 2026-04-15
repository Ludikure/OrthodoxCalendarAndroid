package com.orthodox.calendar.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orthodox.calendar.data.model.AppLanguage
import com.orthodox.calendar.ui.theme.AppColors

@Composable
fun CalendarTitle(
    appTitle: String,
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    val subtitle = when (language) {
        AppLanguage.SR -> "\u0421\u0440\u043F\u0441\u043A\u0430 \u041F\u0440\u0430\u0432\u043E\u0441\u043B\u0430\u0432\u043D\u0430 \u0426\u0440\u043A\u0432\u0430"
        AppLanguage.RU -> "\u0420\u0443\u0441\u0441\u043A\u0430\u044F \u041F\u0440\u0430\u0432\u043E\u0441\u043B\u0430\u0432\u043D\u0430\u044F \u0426\u0435\u0440\u043A\u043E\u0432\u044C"
        AppLanguage.EN, AppLanguage.EN_NC -> "Orthodox Church Calendar"
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(AppColors.warmBg)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "\u2720",
                color = AppColors.crimson,
                fontSize = 20.sp
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = appTitle,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = AppColors.darkText
            )
        }

        Text(
            text = subtitle,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = AppColors.mutedText
        )
    }
}

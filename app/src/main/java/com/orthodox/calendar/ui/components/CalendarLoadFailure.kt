package com.orthodox.calendar.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orthodox.calendar.data.model.AppLanguage
import com.orthodox.calendar.ui.theme.AppColors

/**
 * The "this did not load" state, with a retry. Mirror of iOS
 * `CalendarLoadFailureView`.
 *
 * Public because two screens need the same wording: the month list (a year that
 * failed to download) and the day detail route (a day that cannot be resolved).
 * It used to be private to `CalendarTabScreen`, so the detail route had no
 * failure UI at all and spun for ever instead.
 */
@Composable
fun CalendarLoadFailureView(
    message: String,
    retryLabel: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "⛪", fontSize = 40.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            fontSize = 16.sp,
            color = AppColors.bodyText,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = retryLabel,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.warmBg,
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(AppColors.crimson)
                .clickable { onRetry() }
                .padding(horizontal = 24.dp, vertical = 10.dp)
        )
    }
}

/**
 * The four strings these views fall back to when the bundle lacks the key.
 * Kept here rather than in the shared `ui` strings so the two apps' localization
 * bundles stay byte-identical.
 */
internal fun defaultOfflineMessage(language: AppLanguage): String =
    when (language) {
        AppLanguage.SR -> "Немогуће учитавање података. Проверите везу."
        AppLanguage.RU -> "Не удалось загрузить данные. Проверьте соединение."
        AppLanguage.EN, AppLanguage.EN_NC -> "Couldn't load data. Check your connection."
    }

/** A year the archive does not cover, as opposed to one that failed to download. */
internal fun defaultNoDataMessage(language: AppLanguage, year: Int): String =
    when (language) {
        AppLanguage.SR -> "Нема података за $year. годину."
        AppLanguage.RU -> "Нет данных за $year год."
        AppLanguage.EN, AppLanguage.EN_NC -> "No calendar data for $year."
    }

internal fun defaultRetryLabel(language: AppLanguage): String =
    when (language) {
        AppLanguage.SR -> "Покушај поново"
        AppLanguage.RU -> "Повторить"
        AppLanguage.EN, AppLanguage.EN_NC -> "Retry"
    }

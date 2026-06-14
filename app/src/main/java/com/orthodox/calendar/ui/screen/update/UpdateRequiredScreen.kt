package com.orthodox.calendar.ui.screen.update

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler
import com.orthodox.calendar.data.model.AppLanguage
import com.orthodox.calendar.data.model.LocalizationBundle
import com.orthodox.calendar.ui.theme.AppColors

/**
 * Full-screen, non-dismissable gate shown when the installed version is below the
 * server-required minimum. The only action is to open the Play Store.
 *
 * Mirror of iOS `Views/UpdateRequiredView.swift`.
 */
@Composable
fun UpdateRequiredScreen(
    storeUrl: String?,
    localization: LocalizationBundle?,
    language: AppLanguage,
    onUpdate: () -> Unit
) {
    // Block the system back button — the gate is non-dismissable.
    BackHandler(enabled = true) { /* swallow */ }

    val title = localization?.ui?.updateRequiredTitle ?: defaultTitle(language)
    val message = localization?.ui?.updateRequiredMessage ?: defaultMessage(language)
    val button = localization?.ui?.updateButton ?: defaultButton(language)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.warmBg)
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "✝", fontSize = 40.sp, color = AppColors.crimson)
        Spacer(modifier = Modifier.height(20.dp))
        Text(text = "⬇", fontSize = 52.sp, color = AppColors.crimson)
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = title,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            color = AppColors.darkText,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = message,
            fontSize = 16.sp,
            color = AppColors.mutedText,
            textAlign = TextAlign.Center
        )

        if (storeUrl != null) {
            Spacer(modifier = Modifier.height(36.dp))
            Text(
                text = button,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.warmBg,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(AppColors.crimson)
                    .clickable { onUpdate() }
                    .padding(vertical = 14.dp)
            )
        }
    }
}

private fun defaultTitle(language: AppLanguage) = when (language) {
    AppLanguage.SR -> "Потребно ажурирање"
    AppLanguage.RU -> "Требуется обновление"
    AppLanguage.EN, AppLanguage.EN_NC -> "Update Required"
}

private fun defaultMessage(language: AppLanguage) = when (language) {
    AppLanguage.SR -> "Нова верзија је потребна за наставак. Молимо ажурирајте апликацију."
    AppLanguage.RU -> "Для продолжения требуется новая версия. Пожалуйста, обновите приложение."
    AppLanguage.EN, AppLanguage.EN_NC ->
        "A new version is required to continue. Please update the app."
}

private fun defaultButton(language: AppLanguage) = when (language) {
    AppLanguage.SR -> "Ажурирај"
    AppLanguage.RU -> "Обновить"
    AppLanguage.EN, AppLanguage.EN_NC -> "Update"
}

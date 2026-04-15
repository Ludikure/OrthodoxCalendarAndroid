package com.orthodox.calendar.ui.screen.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orthodox.calendar.data.model.AppLanguage
import com.orthodox.calendar.ui.theme.AppColors

@Composable
fun LanguagePicker(
    selectedLanguage: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier
) {
    AppLanguage.entries.forEach { lang ->
        Row(
            modifier = modifier
                .fillMaxWidth()
                .clickable { onLanguageSelected(lang) }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = lang.displayName,
                fontSize = 16.sp,
                color = AppColors.darkText
            )
            Spacer(modifier = Modifier.weight(1f))
            RadioButton(
                selected = selectedLanguage == lang,
                onClick = { onLanguageSelected(lang) },
                colors = RadioButtonDefaults.colors(
                    selectedColor = AppColors.crimson
                )
            )
        }
    }
}

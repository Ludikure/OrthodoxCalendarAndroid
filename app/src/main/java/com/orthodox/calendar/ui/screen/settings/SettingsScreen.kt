package com.orthodox.calendar.ui.screen.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orthodox.calendar.BuildConfig
import com.orthodox.calendar.data.model.AppLanguage
import com.orthodox.calendar.data.model.AppTheme
import com.orthodox.calendar.data.model.BibleTranslation
import com.orthodox.calendar.data.model.LocalizationBundle
import com.orthodox.calendar.ui.theme.AppColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    language: AppLanguage,
    theme: AppTheme,
    bibleTranslation: BibleTranslation,
    localization: LocalizationBundle,
    onLanguageChanged: (AppLanguage) -> Unit,
    onThemeChanged: (AppTheme) -> Unit,
    onBibleTranslationChanged: (BibleTranslation) -> Unit,
    onAboutClick: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val aboutLabel = when (language) {
        AppLanguage.SR -> "\u041E \u0430\u043F\u043B\u0438\u043A\u0430\u0446\u0438\u0458\u0438"
        AppLanguage.RU -> "\u041E \u043F\u0440\u0438\u043B\u043E\u0436\u0435\u043D\u0438\u0438"
        AppLanguage.EN, AppLanguage.EN_NC -> "About"
    }
    val versionLabel = when (language) {
        AppLanguage.SR -> "\u0412\u0435\u0440\u0437\u0438\u0458\u0430"
        AppLanguage.RU -> "\u0412\u0435\u0440\u0441\u0438\u044F"
        AppLanguage.EN, AppLanguage.EN_NC -> "Version"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(localization.ui.settingsLabel) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Language section
            SectionHeader(title = localization.ui.settingsLabel)
            LanguagePicker(
                selectedLanguage = language,
                onLanguageSelected = onLanguageChanged
            )

            HorizontalDivider()

            // Theme section
            SectionHeader(title = AppTheme.sectionTitle(language))
            AppTheme.entries.forEach { t ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onThemeChanged(t) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = t.displayName(language),
                        fontSize = 16.sp,
                        color = AppColors.darkText
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    RadioButton(
                        selected = theme == t,
                        onClick = { onThemeChanged(t) },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = AppColors.crimson
                        )
                    )
                }
            }

            // English New Testament translation (KJV/WEB). Only relevant to the
            // English locales; the Old Testament always uses the Septuagint.
            if (language == AppLanguage.EN || language == AppLanguage.EN_NC) {
                HorizontalDivider()

                SectionHeader(title = "Bible Translation")
                BibleTranslation.entries.forEach { t ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onBibleTranslationChanged(t) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = t.displayName,
                            fontSize = 16.sp,
                            color = AppColors.darkText
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        RadioButton(
                            selected = bibleTranslation == t,
                            onClick = { onBibleTranslationChanged(t) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = AppColors.crimson
                            )
                        )
                    }
                }
                Text(
                    text = "New Testament wording. The Old Testament always uses the Septuagint.",
                    fontSize = 12.sp,
                    color = AppColors.mutedText,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            HorizontalDivider()

            // About link
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onAboutClick() }
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = aboutLabel,
                    fontSize = 16.sp,
                    color = AppColors.darkText
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(text = "\u203A", fontSize = 20.sp, color = AppColors.mutedText)
            }

            HorizontalDivider()

            // Version
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = versionLabel,
                    fontSize = 16.sp,
                    color = AppColors.darkText
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = BuildConfig.VERSION_NAME,
                    fontSize = 16.sp,
                    color = AppColors.mutedText
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        color = AppColors.mutedText,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

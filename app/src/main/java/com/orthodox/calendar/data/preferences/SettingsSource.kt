package com.orthodox.calendar.data.preferences

import com.orthodox.calendar.data.model.AppLanguage
import com.orthodox.calendar.data.model.AppTheme
import com.orthodox.calendar.data.model.BibleTranslation
import kotlinx.coroutines.flow.Flow

/**
 * The three settings the app persists.
 *
 * [AppPreferences] is the only production implementation; the interface exists
 * so the ViewModel's startup path — which has to survive a preferences file it
 * cannot read — can be tested against a fake instead of a real `DataStore`.
 */
interface SettingsSource {
    val languageFlow: Flow<AppLanguage>
    val themeFlow: Flow<AppTheme>
    val bibleTranslationFlow: Flow<BibleTranslation>

    suspend fun setLanguage(language: AppLanguage)
    suspend fun setTheme(theme: AppTheme)
    suspend fun setBibleTranslation(translation: BibleTranslation)
}

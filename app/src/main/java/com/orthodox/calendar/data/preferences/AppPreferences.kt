package com.orthodox.calendar.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.orthodox.calendar.data.model.AppLanguage
import com.orthodox.calendar.data.model.AppTheme
import com.orthodox.calendar.data.model.BibleTranslation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class AppPreferences(private val context: Context) {

    companion object {
        private val LANGUAGE_KEY = stringPreferencesKey("app_language")
        private val THEME_KEY = stringPreferencesKey("app_theme")
        private val BIBLE_TRANSLATION_KEY = stringPreferencesKey("bible_translation")
    }

    val languageFlow: Flow<AppLanguage> = context.dataStore.data.map { prefs ->
        val code = prefs[LANGUAGE_KEY] ?: "sr"
        AppLanguage.fromCode(code)
    }

    val themeFlow: Flow<AppTheme> = context.dataStore.data.map { prefs ->
        val code = prefs[THEME_KEY] ?: "system"
        AppTheme.fromCode(code)
    }

    val bibleTranslationFlow: Flow<BibleTranslation> = context.dataStore.data.map { prefs ->
        val code = prefs[BIBLE_TRANSLATION_KEY] ?: "kjv"
        BibleTranslation.fromCode(code)
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.dataStore.edit { prefs ->
            prefs[LANGUAGE_KEY] = language.code
        }
    }

    suspend fun setTheme(theme: AppTheme) {
        context.dataStore.edit { prefs ->
            prefs[THEME_KEY] = theme.code
        }
    }

    suspend fun setBibleTranslation(translation: BibleTranslation) {
        context.dataStore.edit { prefs ->
            prefs[BIBLE_TRANSLATION_KEY] = translation.code
        }
    }
}

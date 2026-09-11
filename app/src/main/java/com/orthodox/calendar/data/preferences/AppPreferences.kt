package com.orthodox.calendar.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.orthodox.calendar.data.model.AppLanguage
import com.orthodox.calendar.data.model.AppTheme
import com.orthodox.calendar.data.model.BibleTranslation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * The three persisted settings. Reads are raw `DataStore` flows and can throw;
 * callers that cannot afford to fail use [firstOrDefault] rather than `first()`.
 * Writes swallow their own `IOException` — see [attemptWrite].
 */
class AppPreferences(private val context: Context) : SettingsSource {

    companion object {
        private val LANGUAGE_KEY = stringPreferencesKey("app_language")
        private val THEME_KEY = stringPreferencesKey("app_theme")
        private val BIBLE_TRANSLATION_KEY = stringPreferencesKey("bible_translation")
    }

    override val languageFlow: Flow<AppLanguage> = context.dataStore.data.map { prefs ->
        val code = prefs[LANGUAGE_KEY] ?: "sr"
        AppLanguage.fromCode(code)
    }

    override val themeFlow: Flow<AppTheme> = context.dataStore.data.map { prefs ->
        val code = prefs[THEME_KEY] ?: "system"
        AppTheme.fromCode(code)
    }

    override val bibleTranslationFlow: Flow<BibleTranslation> = context.dataStore.data.map { prefs ->
        val code = prefs[BIBLE_TRANSLATION_KEY] ?: "kjv"
        BibleTranslation.fromCode(code)
    }

    override suspend fun setLanguage(language: AppLanguage) = attemptWrite { prefs ->
        prefs[LANGUAGE_KEY] = language.code
    }

    override suspend fun setTheme(theme: AppTheme) = attemptWrite { prefs ->
        prefs[THEME_KEY] = theme.code
    }

    override suspend fun setBibleTranslation(translation: BibleTranslation) = attemptWrite { prefs ->
        prefs[BIBLE_TRANSLATION_KEY] = translation.code
    }

    /**
     * A write that fails (full disk, another process holding the file) must not
     * take the caller's coroutine down with it. The screens apply their change
     * to in-memory state *after* calling one of these setters, so an exception
     * here used to mean the user tapped a language and the app did nothing at
     * all, silently, for the rest of the session. Losing the persistence is the
     * lesser failure; the choice still stands until the app restarts.
     */
    private suspend fun attemptWrite(block: suspend (MutablePreferences) -> Unit) {
        try {
            context.dataStore.edit(block)
        } catch (e: IOException) {
            // Deliberately swallowed; see the comment above. IOException only —
            // it is what DataStore throws for a failed write, CorruptionException
            // included. Anything else is a bug and should surface, not vanish.
        }
    }
}

package com.orthodox.calendar.data.model

enum class AppTheme(val code: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark");

    fun displayName(language: AppLanguage): String = when (this) {
        SYSTEM -> when (language) {
            AppLanguage.SR -> "\u0421\u0438\u0441\u0442\u0435\u043C\u0441\u043A\u0438"
            AppLanguage.RU -> "\u0421\u0438\u0441\u0442\u0435\u043C\u043D\u0430\u044F"
            AppLanguage.EN, AppLanguage.EN_NC -> "System"
        }
        LIGHT -> when (language) {
            AppLanguage.SR -> "\u0421\u0432\u0435\u0442\u043B\u0430"
            AppLanguage.RU -> "\u0421\u0432\u0435\u0442\u043B\u0430\u044F"
            AppLanguage.EN, AppLanguage.EN_NC -> "Light"
        }
        DARK -> when (language) {
            AppLanguage.SR -> "\u0422\u0430\u043C\u043D\u0430"
            AppLanguage.RU -> "\u0422\u0451\u043C\u043D\u0430\u044F"
            AppLanguage.EN, AppLanguage.EN_NC -> "Dark"
        }
    }

    companion object {
        fun fromCode(code: String): AppTheme =
            entries.firstOrNull { it.code == code } ?: SYSTEM

        fun sectionTitle(language: AppLanguage): String = when (language) {
            AppLanguage.SR -> "\u0422\u0435\u043C\u0430"
            AppLanguage.RU -> "\u0422\u0435\u043C\u0430"
            AppLanguage.EN, AppLanguage.EN_NC -> "Theme"
        }
    }
}

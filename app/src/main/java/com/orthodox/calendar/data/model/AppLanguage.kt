package com.orthodox.calendar.data.model

enum class AppLanguage(val code: String) {
    SR("sr"),
    RU("ru"),
    EN("en"),
    EN_NC("en_nc");

    val displayName: String
        get() = when (this) {
            SR -> "Srpski"
            RU -> "Russkiy"
            EN -> "English (Old Calendar)"
            EN_NC -> "English (New Calendar)"
        }

    /** The localization file to load (en_nc shares en.json) */
    val localizationFile: String
        get() = when (this) {
            EN_NC -> "en"
            else -> code
        }

    companion object {
        fun fromCode(code: String): AppLanguage =
            entries.firstOrNull { it.code == code } ?: SR
    }
}

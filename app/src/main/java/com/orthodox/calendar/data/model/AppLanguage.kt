package com.orthodox.calendar.data.model

enum class AppLanguage(val code: String) {
    SR("sr"),
    RU("ru"),
    EN("en"),
    EN_NC("en_nc");

    val displayName: String
        get() = when (this) {
            SR -> "Српски"
            RU -> "Русский"
            EN, EN_NC -> "English"
        }

    /**
     * Whose calendar each option follows, under its name in the picker and
     * under the app title. Two English calendars already exist and a Greek
     * one may follow, so the language alone no longer says which it is.
     * Mirror of AppLanguage.churchName on iOS.
     */
    val churchName: String
        get() = when (this) {
            SR -> "Српска Православна Црква (СПЦ)"
            RU -> "Русская Православная Церковь (РПЦ)"
            EN -> "ROCOR · Old Calendar"
            EN_NC -> "OCA · New Calendar"
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

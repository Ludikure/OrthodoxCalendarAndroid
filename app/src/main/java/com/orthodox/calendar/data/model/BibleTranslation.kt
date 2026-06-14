package com.orthodox.calendar.data.model

/**
 * English New Testament translation choice. The Old Testament is always the
 * Septuagint (Brenton), so this only affects NT readings.
 *
 * Mirror of iOS `BibleTranslation` (Localization/LocalizationManager.swift).
 */
enum class BibleTranslation(val code: String) {
    KJV("kjv"),
    WEB("web");

    val displayName: String
        get() = when (this) {
            KJV -> "King James Version"
            WEB -> "World English Bible"
        }

    companion object {
        fun fromCode(code: String): BibleTranslation =
            entries.firstOrNull { it.code == code } ?: KJV
    }
}

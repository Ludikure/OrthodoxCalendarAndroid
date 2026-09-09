package com.orthodox.calendar.ui.util

import com.orthodox.calendar.data.model.AppLanguage

/**
 * Labels for controls whose visible content carries no words — icon buttons and
 * the glyph chevrons in the month header — plus the two that were hardcoded in
 * English and announced verbatim in a Serbian or Russian UI.
 *
 * Kept here rather than in the shared `ui` strings so the two apps' localization
 * bundles stay byte-identical.
 */

fun backLabel(language: AppLanguage): String = when (language) {
    AppLanguage.SR -> "Назад"
    AppLanguage.RU -> "Назад"
    AppLanguage.EN, AppLanguage.EN_NC -> "Back"
}

fun shareLabel(language: AppLanguage): String = when (language) {
    AppLanguage.SR -> "Подели"
    AppLanguage.RU -> "Поделиться"
    AppLanguage.EN, AppLanguage.EN_NC -> "Share"
}

fun previousMonthLabel(language: AppLanguage): String = when (language) {
    AppLanguage.SR -> "Претходни месец"
    AppLanguage.RU -> "Предыдущий месяц"
    AppLanguage.EN, AppLanguage.EN_NC -> "Previous month"
}

fun nextMonthLabel(language: AppLanguage): String = when (language) {
    AppLanguage.SR -> "Следећи месец"
    AppLanguage.RU -> "Следующий месяц"
    AppLanguage.EN, AppLanguage.EN_NC -> "Next month"
}

fun chooseMonthLabel(language: AppLanguage): String = when (language) {
    AppLanguage.SR -> "Изабери месец"
    AppLanguage.RU -> "Выбрать месяц"
    AppLanguage.EN, AppLanguage.EN_NC -> "Choose month"
}

fun listViewLabel(language: AppLanguage): String = when (language) {
    AppLanguage.SR -> "Приказ листе"
    AppLanguage.RU -> "Список"
    AppLanguage.EN, AppLanguage.EN_NC -> "List view"
}

fun gridViewLabel(language: AppLanguage): String = when (language) {
    AppLanguage.SR -> "Приказ мреже"
    AppLanguage.RU -> "Сетка"
    AppLanguage.EN, AppLanguage.EN_NC -> "Grid view"
}

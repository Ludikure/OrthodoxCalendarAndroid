package com.orthodox.calendar.data.model

import kotlinx.serialization.Serializable

@Serializable
data class LocalizationBundle(
    val language: String,
    val displayName: String,
    val script: String,
    val ui: UILabels,
    val feastNames: Map<String, String> = emptyMap(),
    val extraFeasts: List<ExtraFeast> = emptyList(),
    val feastTypeOverrides: Map<String, String> = emptyMap(),
    val fastingPeriodNames: Map<String, String> = emptyMap()
)

@Serializable
data class UILabels(
    val appTitle: String,
    val months: List<String>,
    val daysOfWeek: List<String>,
    val daysOfWeekFull: List<String>,
    val julianLabel: String,
    val fastingLabel: String,
    val readingsLabel: String,
    val commemorationsLabel: String,
    val settingsLabel: String,
    val todayLabel: String,
    val feastTypes: Map<String, String> = emptyMap(),
    val fastingTypes: Map<String, String> = emptyMap()
)

@Serializable
data class ExtraFeast(
    val julianMonth: Int,
    val julianDay: Int,
    val name: String,
    val type: String,
    val description: String? = null
)

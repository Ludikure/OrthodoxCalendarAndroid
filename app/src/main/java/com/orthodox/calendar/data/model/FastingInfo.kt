package com.orthodox.calendar.data.model

import kotlinx.serialization.Serializable

@Serializable
data class FastingInfo(
    val type: String,                     // "free", "fish", "dryEating", "hotWithOil", etc.
    val label: String,
    val explanation: String,
    val abbrev: String? = null,
    val icon: String? = null
)

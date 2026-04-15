package com.orthodox.calendar.data.model

import kotlinx.serialization.Serializable

@Serializable
data class CalendarFile(
    val year: Int,
    val locale: String,
    val generatedBy: String,
    val days: Map<String, CalendarDay>
)

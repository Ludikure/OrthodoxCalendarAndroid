package com.orthodox.calendar.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Reflection(
    val source: String,
    val text: String
)

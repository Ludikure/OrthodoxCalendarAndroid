package com.orthodox.calendar.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Feast(
    val name: String,
    val importance: String,               // "great", "bold", "normal"
    val displayRole: String,              // "primary", "secondary", "tertiary"
    val type: String,                     // "feast", "saint", "martyr", "venerable", etc.
    val isSlava: Boolean = false,
    val moveable: Boolean = false,
    val description: String? = null,
    val liturgicalContext: String? = null,
    val position: Int? = null,
    val serbianSaint: Boolean? = null
)

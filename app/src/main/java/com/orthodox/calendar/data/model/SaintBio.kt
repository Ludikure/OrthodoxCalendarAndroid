package com.orthodox.calendar.data.model

import kotlinx.serialization.Serializable

@Serializable
data class SaintBio(
    val title: String,
    val text: String
)

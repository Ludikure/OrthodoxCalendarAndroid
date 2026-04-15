package com.orthodox.calendar.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ScriptureReading(
    val type: String,                     // "apostol", "gospel", "ot"
    val book: String? = null,
    val title: String? = null,
    val reference: String? = null,
    val zachalo: Int? = null,
    val text: String? = null,             // Full scripture text
    val service: String? = null           // "Jutrenya", "Liturgija", etc.
) {
    /** Best available display string for this reading */
    val displayReference: String
        get() = reference ?: title ?: book ?: type
}

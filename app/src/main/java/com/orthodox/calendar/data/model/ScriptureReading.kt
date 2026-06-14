package com.orthodox.calendar.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ScriptureReading(
    val type: String,                     // "apostol", "gospel", "ot"
    val book: String? = null,
    val title: String? = null,
    val reference: String? = null,
    val zachalo: Int? = null,
    val text: String? = null,             // KJV NT / Brenton (Septuagint) OT
    val textWeb: String? = null,          // WEB NT (English only); null otherwise
    val service: String? = null           // "Jutrenya", "Liturgija", etc.
) {
    /** Best available display string for this reading */
    val displayReference: String
        get() = reference ?: title ?: book ?: type

    /**
     * Scripture text for the chosen NT translation. Falls back to [text] when
     * the WEB variant is absent (SR/RU, OT, or days without WEB data).
     * Mirror of iOS `ScriptureReading.text(for:)`.
     */
    fun text(translation: BibleTranslation): String? =
        if (translation == BibleTranslation.WEB && textWeb != null) textWeb else text
}

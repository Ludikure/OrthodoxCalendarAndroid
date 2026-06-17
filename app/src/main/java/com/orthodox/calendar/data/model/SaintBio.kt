package com.orthodox.calendar.data.model

import kotlinx.serialization.Serializable

@Serializable
data class SaintBio(
    val title: String,
    // Empty in deduped bundled data; filled from the bios_<locale> pool via [ref].
    // Always present in API-streamed data.
    val text: String = "",
    // Content hash into bios_<locale>.json (deduped bundled files only).
    val ref: String? = null
)

package com.orthodox.calendar.data.repository

import java.io.File

/**
 * The downloaded years to delete: all but the [keepPerLocale] most recently used
 * per locale. "Used" is the file's modification time, which a download sets and
 * a disk-cache read refreshes (`CalendarRepository.decodeDisk`).
 *
 * This used to keep the [keepPerLocale] highest year *numbers*, which is not the
 * same thing. 2024 — the one downloadable year below the bundled window — was
 * always the lowest, so once twelve later years were on disk, opening 2024
 * downloaded it and the trim that ran straight afterwards deleted it again, on
 * every visit. Paging back to a year after paging forward past it lost that
 * year the same way.
 *
 * Only `calendar_<locale>_<year>.json` files count; anything else in the
 * directory is left alone. Ties in modification time (coarse on some file
 * systems) fall back to the file name, so the choice is deterministic.
 */
internal fun cacheFilesToEvict(files: List<File>, keepPerLocale: Int): List<File> =
    files.mapNotNull { file -> cacheFileLocale(file.name)?.let { locale -> locale to file } }
        .groupBy({ it.first }, { it.second })
        .values
        .flatMap { perLocale ->
            perLocale
                .sortedWith(compareByDescending<File> { it.lastModified() }.thenByDescending { it.name })
                .drop(keepPerLocale)
        }

/** "calendar_en_nc_2031.json" to "en_nc"; null for a file this cache did not write. */
private fun cacheFileLocale(name: String): String? {
    if (!name.startsWith("calendar_") || !name.endsWith(".json")) return null
    val stem = name.removePrefix("calendar_").removeSuffix(".json")
    val locale = stem.substringBeforeLast('_', missingDelimiterValue = "")
    val year = stem.substringAfterLast('_', missingDelimiterValue = "").toIntOrNull()
    return locale.takeIf { it.isNotEmpty() && year != null }
}

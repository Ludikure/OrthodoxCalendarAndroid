package com.orthodox.calendar.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Which downloaded years the disk trim deletes. Real files in a temporary folder
 * with modification times set explicitly: the trim's whole job is to rank by
 * recency, so the test controls the clock it ranks by.
 */
class DiskCacheEvictionTest {

    @get:Rule
    val tmp = TemporaryFolder()

    /** Each file written is a minute more recent than the one before it. */
    private var mtime = 1_700_000_000_000L

    private fun year(name: String): File =
        tmp.newFile(name).apply { setLastModified(mtime); mtime += 60_000 }

    private fun List<File>.names() = map { it.name }.sorted()

    /**
     * The bug: ranking by year number deleted 2024 — the one downloadable year
     * below the bundled window — straight after downloading it, every time.
     */
    @Test
    fun `a year just downloaded survives the trim that follows it`() {
        val browsedEarlier = (2031..2042).map { year("calendar_sr_$it.json") }
        val justDownloaded = year("calendar_sr_2024.json")
        val evicted = cacheFilesToEvict(browsedEarlier + justDownloaded, keepPerLocale = 12)
        assertEquals(listOf("calendar_sr_2031.json"), evicted.names())
    }

    /** Paging forward past a year and coming back to it keeps it. */
    @Test
    fun `a year read again counts as recent`() {
        val years = (2031..2043).map { year("calendar_sr_$it.json") }
        years.first().setLastModified(mtime) // 2031, opened again just now
        val evicted = cacheFilesToEvict(years, keepPerLocale = 12)
        assertEquals(listOf("calendar_sr_2032.json"), evicted.names())
    }

    @Test
    fun `each locale is trimmed on its own`() {
        val en = (2031..2042).map { year("calendar_en_$it.json") }
        val enNc = (2031..2042).map { year("calendar_en_nc_$it.json") }
        // Twelve each: at the limit per locale, though twenty-four in all.
        assertTrue(cacheFilesToEvict(en + enNc, keepPerLocale = 12).isEmpty())
    }

    /** Read as locale "en", en_nc would make en thirteen files and cost it one. */
    @Test
    fun `en_nc is a locale of its own, not en`() {
        val en = (2031..2042).map { year("calendar_en_$it.json") }
        val enNc = year("calendar_en_nc_2031.json")
        assertTrue(cacheFilesToEvict(en + enNc, keepPerLocale = 12).isEmpty())
    }

    @Test
    fun `files the cache did not write are left alone`() {
        val foreign = listOf("notes.txt", "calendar_sr.json", "calendar_sr_x.json", "calendar__2031.json")
            .map { year(it) }
        assertTrue(cacheFilesToEvict(foreign, keepPerLocale = 0).isEmpty())
    }

    @Test
    fun `at or under the limit nothing is evicted`() {
        val years = (2031..2042).map { year("calendar_ru_$it.json") }
        assertTrue(cacheFilesToEvict(years, keepPerLocale = 12).isEmpty())
    }
}

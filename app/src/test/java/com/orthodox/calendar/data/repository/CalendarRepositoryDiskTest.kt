package com.orthodox.calendar.data.repository

import android.content.ComponentCallbacks2
import com.orthodox.calendar.OrthodoxCalendarApp
import com.orthodox.calendar.data.model.CalendarFile
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

/**
 * The repository's disk cache and its invalidation, against a real `filesDir`.
 *
 * Only years outside the bundled window live on disk, so each test writes one —
 * a minimal but valid 2031 — instead of downloading it. Nothing here reaches the
 * network: every load passes `allowNetwork = false`.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CalendarRepositoryDiskTest {

    private lateinit var app: OrthodoxCalendarApp
    private lateinit var cacheDir: File

    @Before
    fun setUp() {
        app = RuntimeEnvironment.getApplication() as OrthodoxCalendarApp
        cacheDir = File(app.filesDir, "calendar_cache").apply {
            deleteRecursively()
            mkdirs()
        }
    }

    private fun writeYear(
        locale: String,
        year: Int,
        mtime: Long = System.currentTimeMillis(),
        name: String = "calendar_${locale}_$year.json"
    ): File =
        File(cacheDir, name).apply {
            writeText(
                """{"year":$year,"locale":"$locale","generatedBy":"test","days":{"01-01":""" +
                    """{"gregorianDate":"$year-01-01","julianDate":"12-19","dayOfWeek":0,""" +
                    """"paschaDistance":-100,"feasts":[],"fasting":{"type":"free","label":"","explanation":""}}}}"""
            )
            setLastModified(mtime)
        }

    private fun emptyYear(year: Int) = CalendarFile(year, "sr", "test", emptyMap())

    @Test
    fun `reading a downloaded year refreshes its place in the disk trim`() = runBlocking<Unit> {
        val longAgo = 1_700_000_000_000L
        val file = writeYear("sr", 2031, mtime = longAgo)
        CalendarRepository(app).load("sr", 2031, allowNetwork = false)
        assertTrue("a disk-cache read must count as use", file.lastModified() > longAgo)
    }

    @Test
    fun `a new archive revision drops years from disk and from memory`() = runBlocking<Unit> {
        val file = writeYear("sr", 2031)
        val repo = CalendarRepository(app)
        repo.load("sr", 2031, allowNetwork = false)
        assertEquals(listOf("calendar_sr_2031"), repo.cachedYearKeys())

        repo.invalidateForNewRevision()

        assertTrue(repo.cachedYearKeys().isEmpty())
        assertFalse(file.exists())
    }

    /**
     * The race: a load reads its year from disk, the revision moves while it is
     * still resolving, and it then stores the superseded year. Clearing the
     * in-flight map did not stop that; the generation check does.
     */
    @Test
    fun `a year read from disk before a new revision is not stored after it`() {
        val repo = CalendarRepository(app)
        val readUnder = repo.currentGeneration()
        repo.invalidateForNewRevision()
        repo.storeIfCurrent("calendar_sr_2031", emptyYear(2031), fromDisk = true, startGeneration = readUnder)
        assertTrue("superseded disk data must not re-enter memory", repo.cachedYearKeys().isEmpty())
    }

    /** Bundled years and fresh downloads are never superseded by a revision. */
    @Test
    fun `a year not read from disk is stored whatever the generation`() {
        val repo = CalendarRepository(app)
        val readUnder = repo.currentGeneration()
        repo.invalidateForNewRevision()
        repo.storeIfCurrent("calendar_sr_2031", emptyYear(2031), fromDisk = false, startGeneration = readUnder)
        assertEquals(listOf("calendar_sr_2031"), repo.cachedYearKeys())
    }

    @Test
    fun `a year read from disk under the current revision is stored`() {
        val repo = CalendarRepository(app)
        repo.storeIfCurrent(
            "calendar_sr_2031", emptyYear(2031), fromDisk = true, startGeneration = repo.currentGeneration()
        )
        assertEquals(listOf("calendar_sr_2031"), repo.cachedYearKeys())
    }

    /** A bundled year's archive copy wins only above the bundle's revision. */
    @Test
    fun `a newer archive copy of a bundled year is preferred over the bundle`() = runBlocking<Unit> {
        val key = "calendar_sr_2026"
        val newer = BundledData.cacheName(key, BundledData.REVISION + 1) + ".json"
        writeYear("sr", 2026, name = newer)
        val file = CalendarRepository(app).load("sr", 2026, allowNetwork = false)
        assertEquals("the one-day copy, not the bundle", 1, file.days.size)
    }

    @Test
    fun `a copy at or below the bundle's revision is ignored and removed`() = runBlocking<Unit> {
        val key = "calendar_sr_2026"
        val stale = writeYear("sr", 2026, name = BundledData.cacheName(key, BundledData.REVISION) + ".json")
        val file = CalendarRepository(app).load("sr", 2026, allowNetwork = false)
        assertTrue("the bundle", file.days.size >= 365)
        assertFalse("a copy that can never win is deleted", stale.exists())
    }

    @Test
    fun `archive copies are not counted by the disk trim`() {
        val copy = File(cacheDir, BundledData.cacheName("calendar_sr_2026", 12) + ".json").apply { writeText("{}") }
        assertTrue(cacheFilesToEvict(listOf(copy), keepPerLocale = 0).isEmpty())
    }

    /**
     * End to end through the Application, at the level a phone on Android 14 or
     * later actually sends. This released nothing before: the level was missing
     * from the release set.
     */
    @Test
    fun `a background memory trim empties the repository`() = runBlocking<Unit> {
        writeYear("sr", 2031)
        app.repository.load("sr", 2031, allowNetwork = false)
        assertFalse(app.repository.cachedYearKeys().isEmpty())

        app.onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_BACKGROUND)

        assertTrue(app.repository.cachedYearKeys().isEmpty())
    }
}

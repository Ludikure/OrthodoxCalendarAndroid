package com.orthodox.calendar.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A bundled year gives way to an archive copy only when the archive is newer
 * than the bundle, and a revision bump never leaves a stale copy in charge.
 *
 * Mirror of `OrthodoxCalendarTests/BundledDataTests.swift`.
 */
class BundledDataTest {

    @Test
    fun `bundle wins without a newer copy`() {
        assertEquals(BundledData.Source.BUNDLE, BundledData.source(bundleRevision = 9, cachedRevision = null))
        assertEquals(BundledData.Source.BUNDLE, BundledData.source(bundleRevision = 9, cachedRevision = 9))
        // A copy downloaded before this app's newer bundle shipped.
        assertEquals(BundledData.Source.BUNDLE, BundledData.source(bundleRevision = 10, cachedRevision = 9))
        assertEquals(BundledData.Source.BUNDLE, BundledData.source(bundleRevision = 10, cachedRevision = 10))
    }

    @Test
    fun `newer copy wins`() {
        assertEquals(BundledData.Source.CACHE, BundledData.source(bundleRevision = 9, cachedRevision = 10))
    }

    @Test
    fun `download only when the server is newer`() {
        // Offline or no config: never.
        assertFalse(BundledData.shouldDownload(bundleRevision = 9, serverRevision = null, cachedRevision = null))
        // Same or older archive than the bundle: never.
        assertFalse(BundledData.shouldDownload(bundleRevision = 9, serverRevision = 9, cachedRevision = null))
        assertFalse(BundledData.shouldDownload(bundleRevision = 10, serverRevision = 9, cachedRevision = null))
        // Newer archive: once.
        assertTrue(BundledData.shouldDownload(bundleRevision = 9, serverRevision = 10, cachedRevision = null))
        assertFalse(BundledData.shouldDownload(bundleRevision = 9, serverRevision = 10, cachedRevision = 10))
        // Bumped again past the copy on disk: fetched again.
        assertTrue(BundledData.shouldDownload(bundleRevision = 9, serverRevision = 11, cachedRevision = 10))
    }

    @Test
    fun `cache name carries the revision`() {
        val name = BundledData.cacheName("calendar_ru_2026", revision = 10)
        assertEquals("calendar_ru_2026.r10", name)
        assertEquals(10, BundledData.revision("$name.json", key = "calendar_ru_2026"))
        // Another locale's or year's copy, and a plain downloaded year, are not it.
        assertNull(BundledData.revision("calendar_ru_2026.json", key = "calendar_ru_2026"))
        assertNull(BundledData.revision("calendar_ru_2027.r10.json", key = "calendar_ru_2026"))
        assertNull(BundledData.revision("calendar_en_nc_2026.r10.json", key = "calendar_en_2026"))
    }

    @Test
    fun `bundle revision is set`() {
        assertTrue(BundledData.REVISION >= 9)
    }
}

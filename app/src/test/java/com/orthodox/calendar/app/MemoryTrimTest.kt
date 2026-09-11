package com.orthodox.calendar.app

import android.content.ComponentCallbacks2
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Which `onTrimMemory` levels release the decoded years.
 *
 * On API 34 and later an app is sent only two levels: TRIM_MEMORY_UI_HIDDEN (20)
 * when its UI leaves the screen, and TRIM_MEMORY_BACKGROUND (40) when its process
 * joins the LRU list. The platform source marks the other five "Apps are not
 * notified of this level since API level 34". BACKGROUND is therefore the only
 * memory signal a current device sends this app, and a release set without it
 * never runs at all. One shipped that way: BACKGROUND was left out on the belief
 * that it shared the value 20 with UI_HIDDEN. It is 40.
 *
 * UI_HIDDEN stays out: it arrives every time the user leaves the app, and they
 * usually come straight back to a cache they would then have to re-decode.
 *
 * Plain JUnit: these are compile-time constants inlined from the SDK, so they
 * carry their real values without Robolectric — the first test proves it.
 */
@Suppress("DEPRECATION") // five of the seven levels are deprecated; see above
class MemoryTrimTest {

    /** The two values the release set was once written against, wrongly. */
    @Test
    fun `ui hidden and background are different levels`() {
        assertEquals(20, ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN)
        assertEquals(40, ComponentCallbacks2.TRIM_MEMORY_BACKGROUND)
    }

    /**
     * The test that would have caught it: of the levels a current device actually
     * delivers, at least one must release, or releaseMemory() is dead code on
     * every phone running Android 14 or later.
     */
    @Test
    fun `on android 14 and later a delivered level releases the cache`() {
        val deliveredSinceApi34 = listOf(
            ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN,
            ComponentCallbacks2.TRIM_MEMORY_BACKGROUND
        )
        assertTrue(
            "API 34+ delivers only UI_HIDDEN and BACKGROUND; if neither releases, nothing does",
            deliveredSinceApi34.any(::shouldReleaseCalendarCache)
        )
    }

    @Test
    fun `background releases the cache`() {
        assertTrue(shouldReleaseCalendarCache(ComponentCallbacks2.TRIM_MEMORY_BACKGROUND))
    }

    /** Leaving the app is not memory pressure; the user usually comes straight back. */
    @Test
    fun `ui hidden keeps the cache`() {
        assertFalse(shouldReleaseCalendarCache(ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN))
    }

    /** What devices before API 34 still send, and should still act on. */
    @Test
    fun `the severe levels older platforms send release the cache`() {
        assertTrue(shouldReleaseCalendarCache(ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW))
        assertTrue(shouldReleaseCalendarCache(ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL))
        assertTrue(shouldReleaseCalendarCache(ComponentCallbacks2.TRIM_MEMORY_MODERATE))
        assertTrue(shouldReleaseCalendarCache(ComponentCallbacks2.TRIM_MEMORY_COMPLETE))
    }

    @Test
    fun `the mildest running level keeps the cache`() {
        assertFalse(shouldReleaseCalendarCache(ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE))
    }

    @Test
    fun `unrelated levels do nothing`() {
        assertFalse(shouldReleaseCalendarCache(0))
        assertFalse(shouldReleaseCalendarCache(-1))
        assertFalse(shouldReleaseCalendarCache(Int.MAX_VALUE))
    }
}

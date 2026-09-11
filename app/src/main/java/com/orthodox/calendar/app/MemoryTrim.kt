package com.orthodox.calendar.app

import android.content.ComponentCallbacks2

/**
 * Whether the system's `onTrimMemory` [level] means the decoded years should go.
 *
 * On API 34 and later an app is sent only two levels: TRIM_MEMORY_UI_HIDDEN (20)
 * when its UI leaves the screen, and TRIM_MEMORY_BACKGROUND (40) when its process
 * joins the LRU list — "a good opportunity to clean up resources that can
 * efficiently and quickly be re-built if the user returns to the app", which is
 * exactly what these years are. The platform marks the other five levels "Apps
 * are not notified of this level since API level 34"; they stay in the list only
 * for devices older than that.
 *
 * So BACKGROUND is the level that matters on a current phone. It was once left
 * out on the belief that it shared the value 20 with UI_HIDDEN, and without it
 * the release never ran on Android 14 or later at all. MemoryTrimTest pins both
 * the values and that a delivered level releases.
 *
 * UI_HIDDEN stays out: it arrives every time the user leaves the app, and they
 * usually come straight back to a cache they would then re-decode (the month on
 * screen survives a release regardless — it lives in the ViewModel's state). The
 * mildest running level stays out too. An explicit list rather than a `>=`
 * comparison because it reads against the table above, not because the levels
 * are unordered: they are ordered, but `>= BACKGROUND` would miss the two running
 * levels an older device sends while the app is in the foreground.
 */
@Suppress("DEPRECATION") // five of these are deprecated since API 34; see above
internal fun shouldReleaseCalendarCache(level: Int): Boolean = when (level) {
    ComponentCallbacks2.TRIM_MEMORY_BACKGROUND,
    ComponentCallbacks2.TRIM_MEMORY_MODERATE,
    ComponentCallbacks2.TRIM_MEMORY_COMPLETE,
    ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW,
    ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL -> true
    else -> false
}

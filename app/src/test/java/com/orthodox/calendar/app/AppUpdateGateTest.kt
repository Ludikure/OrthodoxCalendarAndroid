package com.orthodox.calendar.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The forced-update gate is the one place where a wrong comparison either
 * bricks a working app behind an update screen or lets an incompatible client
 * keep running. String comparison would order "1.10.0" below "1.9.0".
 *
 * Mirror of `OrthodoxCalendarTests/AppUpdateGateTests.swift`.
 */
class AppUpdateGateTest {

    @Test
    fun `dotted numeric compare`() {
        assertTrue(AppUpdateGate.isOlder("1.3.0", "1.3.2"))
        assertTrue(AppUpdateGate.isOlder("1.3.0", "1.4.0"))
        assertFalse(AppUpdateGate.isOlder("1.4.0", "1.4.0"))
        assertFalse(AppUpdateGate.isOlder("1.4.0", "1.3.2"))
        assertFalse(AppUpdateGate.isOlder("1.5.0", "1.4.0"))
        // The lexicographic trap: 1.10 is newer than 1.9, not older.
        assertFalse(AppUpdateGate.isOlder("1.10.0", "1.9.0"))
        assertTrue(AppUpdateGate.isOlder("1.9.0", "1.10.0"))
    }

    @Test
    fun `missing components count as zero`() {
        assertFalse(AppUpdateGate.isOlder("1.4", "1.4.0"))
        assertFalse(AppUpdateGate.isOlder("1.4.0.0", "1.4.0"))
        assertTrue(AppUpdateGate.isOlder("1.4.0", "1.4.1"))
        assertTrue(AppUpdateGate.isOlder("1", "1.2.0"))
        assertFalse(AppUpdateGate.isOlder("1.3", "1.3.0"))
        assertTrue(AppUpdateGate.isOlder("1.3", "1.3.1"))
    }

    @Test
    fun `garbage is not older than a real minimum`() {
        // A version we cannot parse must not lock the user out of the app.
        assertFalse(AppUpdateGate.isOlder("beta", "1.4.0"))
        assertFalse(AppUpdateGate.isOlder("1.5.1", "not-a-version"))
        assertFalse(AppUpdateGate.isOlder("", "1.4.0"))
        // Was already false under the old "non-numeric counts as 0" rule
        // ("1.x" -> [1, 0] == [1, 0]); it must stay false under this one.
        assertFalse(AppUpdateGate.isOlder("1.x", "1.0"))
    }
}

package com.orthodox.calendar.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdateGateTest {

    @Test
    fun `older patch and minor are older`() {
        assertTrue(AppUpdateGate.isOlder("1.3.0", "1.3.2"))
        assertTrue(AppUpdateGate.isOlder("1.3.0", "1.4.0"))
        assertTrue(AppUpdateGate.isOlder("1.9.0", "1.10.0"))
    }

    @Test
    fun `equal version is not older`() {
        assertFalse(AppUpdateGate.isOlder("1.3.2", "1.3.2"))
    }

    @Test
    fun `newer version is not older`() {
        assertFalse(AppUpdateGate.isOlder("1.4.0", "1.3.2"))
        assertFalse(AppUpdateGate.isOlder("1.10.0", "1.9.0"))
    }

    @Test
    fun `missing and non-numeric components count as zero`() {
        assertFalse(AppUpdateGate.isOlder("1.3", "1.3.0"))
        assertTrue(AppUpdateGate.isOlder("1.3", "1.3.1"))
        assertFalse(AppUpdateGate.isOlder("1.x", "1.0"))
    }
}

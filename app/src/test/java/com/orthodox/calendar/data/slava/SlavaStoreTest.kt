package com.orthodox.calendar.data.slava

import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** The slava settings survive a restart, and only a real change reschedules. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SlavaStoreTest {
    private val prefs = RuntimeEnvironment.getApplication()
        .getSharedPreferences("slava_test", Context.MODE_PRIVATE)

    @Before
    fun clear() {
        prefs.edit().clear().commit()
    }

    @Test
    fun `settings persist across instances`() {
        val store = SlavaStore(prefs)
        assertNull(store.settings.value.mine)
        val slava = SlavaCatalog.fixed.first { it.name == "Никољдан" }
        store.update { it.copy(mine = slava, reminderMinutes = 8 * 60) }
        val reopened = SlavaStore(prefs)
        assertEquals(slava, reopened.settings.value.mine)
        assertEquals(8 * 60, reopened.settings.value.reminderMinutes)
    }

    @Test
    fun `only a change notifies`() {
        val store = SlavaStore(prefs)
        var changes = 0
        store.onChange = { changes++ }
        store.update { it }
        assertEquals(0, changes)
        store.update { it.copy(remindFriends = false) }
        store.update { it.copy(remindFriends = false) }
        assertEquals(1, changes)
    }
}

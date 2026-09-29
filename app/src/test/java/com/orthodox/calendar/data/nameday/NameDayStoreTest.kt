package com.orthodox.calendar.data.nameday

import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** The name-day settings survive a restart, only a real change reschedules,
 *  and the catalog loads from the app's assets. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NameDayStoreTest {
    private val app = RuntimeEnvironment.getApplication()
    private val prefs = app.getSharedPreferences("imeniny_test", Context.MODE_PRIVATE)

    private val choice = NameDayChoice("Ваня", "Иоанн", "Иоанна Богослова", NameDayAnchor.Julian(5, 8), 5, 1)

    @Before
    fun clear() {
        prefs.edit().clear().commit()
    }

    @Test
    fun `settings persist across instances`() {
        val store = NameDayStore(prefs)
        assertNull(store.settings.value.mine)
        store.update { it.copy(mine = choice, includeNewMartyrs = true, reminderMinutes = 8 * 60) }
        val reopened = NameDayStore(prefs).settings.value
        assertEquals(choice, reopened.mine)
        assertTrue(reopened.includeNewMartyrs)
        assertEquals(8 * 60, reopened.reminderMinutes)
    }

    @Test
    fun `only a change notifies`() {
        val store = NameDayStore(prefs)
        var changes = 0
        store.onChange = { changes++ }
        store.update { it.copy(mine = choice) }
        store.update { it.copy(mine = choice) }
        assertEquals(1, changes)
    }

    @Test
    fun `unreadable settings start fresh`() {
        prefs.edit().putString("settings", "{not json").commit()
        assertEquals(NameDaySettings(), NameDayStore(prefs).settings.value)
    }

    @Test
    fun `catalog loads from assets`() {
        val catalog = NameDayCatalog.shared(app)
        assertTrue(catalog.names.size > 1000)
        assertEquals(listOf("Иоанн"), catalog.churchForms("Иван"))
    }
}

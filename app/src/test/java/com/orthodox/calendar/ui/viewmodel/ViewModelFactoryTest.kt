package com.orthodox.calendar.ui.viewmodel

import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModelProvider
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The ViewModel built the way the app builds it: through the activity's default
 * factory, which looks up an `(Application)` constructor by reflection.
 *
 * Every other test in this package calls the constructor directly with fakes,
 * so none of them notices when that reflective constructor disappears — and when
 * it does, `viewModel()` in MainActivity throws "Cannot create an instance of
 * class CalendarViewModel" on every launch. It disappeared once already: giving
 * the constructor defaulted dependencies for testability removed it, because
 * Kotlin emits no `(Application)` overload for a constructor with default
 * arguments unless the constructor is `@JvmOverloads`.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ViewModelFactoryTest {

    @Test
    fun `the activity's default factory can build the calendar view model`() {
        val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        val activity = controller.get()
        try {
            // Same path as `viewModel()` in MainActivity: the activity is the
            // ViewModelStoreOwner and supplies the default factory.
            val vm = ViewModelProvider(activity)[CalendarViewModel::class.java]
            assertNotNull(vm)
        } finally {
            // onCleared, which cancels viewModelScope and the midnight ticker in it.
            activity.viewModelStore.clear()
            controller.destroy()
        }
    }
}

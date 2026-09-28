package com.orthodox.calendar

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toDrawable
import androidx.core.net.toUri
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.orthodox.calendar.ui.theme.LocalIsDarkTheme
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.orthodox.calendar.app.ReviewPrompt
import com.orthodox.calendar.ui.navigation.Routes
import kotlinx.coroutines.delay
import com.orthodox.calendar.ui.navigation.NavGraph
import com.orthodox.calendar.ui.screen.update.UpdateRequiredScreen
import com.orthodox.calendar.ui.theme.OrthodoxCalendarTheme
import com.orthodox.calendar.ui.viewmodel.CalendarViewModel
import com.orthodox.calendar.ui.util.toIsoDate
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.LocalDate

class MainActivity : ComponentActivity() {
    /** Widget taps (ACTION_OPEN_TODAY) not yet acted on, counted so a second tap
     *  while the app is open opens today again. */
    private val openTodayRequests = MutableStateFlow(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as OrthodoxCalendarApp
        val repository = app.repository
        // Process-scoped: remembering the gate in the composition let a rotation
        // clear a hard update block and refetch /api/config every time.
        val updateGate = app.updateGate
        val slavaStore = app.slavaStore
        val slavaReminders = app.slavaReminders
        val widgetSync = app.widgetSync
        // A restored activity has already acted on the tap that launched it.
        if (savedInstanceState == null && intent?.action == ACTION_OPEN_TODAY) {
            openTodayRequests.value++
        }

        setContent {
            val viewModel: CalendarViewModel = viewModel()
            val uiState by viewModel.uiState.collectAsState()
            val navController = rememberNavController()
            val context = LocalContext.current
            val reviewPrompt = remember { ReviewPrompt(context) }
            var foregrounds by remember { mutableIntStateOf(0) }

            // Server-controlled minimum-version gate (fail-open).
            val mustUpdate by updateGate.mustUpdate.collectAsState()
            val storeUrl by updateGate.storeUrl.collectAsState()
            LaunchedEffect(Unit) { updateGate.check() }

            // Slava reminders exist only in Serbian: a language change schedules
            // or clears them. The reschedule reads the persisted language itself,
            // so the default this state starts with before preferences load
            // cannot schedule anything the saved language would not.
            LaunchedEffect(uiState.language) { slavaReminders.update() }

            // The widgets' snapshot is rewritten at launch, on return to the
            // foreground, on a language change and when the current year loads
            // (slava changes reach it through the store). Always after the
            // calendar's own load has finished: a local-only read of the same
            // year racing it would decode the year and its text pool twice.
            var widgetPending by remember { mutableStateOf(true) }
            LaunchedEffect(uiState.language) { widgetPending = true }
            val currentYearLoaded = !uiState.isLoading &&
                uiState.loadedYear == LocalDate.now().year &&
                uiState.loadedLocale == uiState.language.code
            LaunchedEffect(currentYearLoaded) { if (currentYearLoaded) widgetPending = true }
            val calendarSettled = !uiState.isLoading && uiState.localization != null &&
                uiState.loadedLocale == uiState.language.code
            LaunchedEffect(widgetPending, calendarSettled) {
                if (widgetPending && calendarSettled) {
                    widgetPending = false
                    widgetSync.refresh(uiState.language)
                }
            }

            // Re-read the day whenever the activity comes back. The ViewModel's
            // midnight tick is a coroutine delay, and that runs on uptime, which
            // stops in deep sleep: a phone left overnight with the app open woke
            // the tick hours late and showed yesterday as today. Coming back to
            // the app is when that must be right, so the date is re-checked here
            // and the tick re-armed from the wall clock.
            LifecycleStartEffect(viewModel) {
                viewModel.refreshToday()
                reviewPrompt.recordActive()
                // A reminder a year out is scheduled for its date; coming back
                // to the app tops the alarms up with the next occurrence.
                slavaReminders.update()
                widgetPending = true
                foregrounds++
                onStopOrDispose { }
            }

            // A widget tap: the calendar on today, with today's detail open —
            // once the splash has handed over to the calendar, so the detail is
            // not buried under it. Mirrors iOS `CalendarViewModel.openToday()`.
            val openToday by openTodayRequests.collectAsState()
            var openedToday by remember { mutableIntStateOf(0) }
            val currentEntry by navController.currentBackStackEntryAsState()
            val route = currentEntry?.destination?.route
            val navReady = route != null && route != Routes.Splash.route
            LaunchedEffect(openToday, navReady) {
                if (openToday <= openedToday || !navReady) return@LaunchedEffect
                openedToday = openToday
                viewModel.goToToday()
                navController.navigate(Routes.DayDetail.createRoute(LocalDate.now().toIsoDate())) {
                    popUpTo(Routes.Calendar.route)
                }
            }

            OrthodoxCalendarTheme(appTheme = uiState.theme) {
                // The theme the app resolved, not the one the system is in:
                // see syncWindowBackground.
                val isDarkTheme = LocalIsDarkTheme.current
                LaunchedEffect(isDarkTheme) {
                    this@MainActivity.syncWindowBackground(isDarkTheme)
                }

                if (mustUpdate) {
                    UpdateRequiredScreen(
                        storeUrl = storeUrl,
                        localization = uiState.localization,
                        language = uiState.language,
                        onUpdate = {
                            // The URL comes from the server, and a device may
                            // have nothing that handles it — an unguarded
                            // startActivity takes the app down on the one screen
                            // whose whole purpose is to get the user unstuck.
                            storeUrl?.let { url ->
                                runCatching {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
                                }
                            }
                        }
                    )
                } else {
                    // The rating ask waits a moment after the calendar appears
                    // and only fires there, never over a day being read; leaving
                    // the calendar within the wait cancels it.
                    val backStack by navController.currentBackStackEntryAsState()
                    val onCalendar = backStack?.destination?.route == Routes.Calendar.route
                    LaunchedEffect(foregrounds, onCalendar) {
                        if (!onCalendar || !reviewPrompt.shouldPrompt) return@LaunchedEffect
                        delay(2_000)
                        reviewPrompt.request(this@MainActivity)
                    }

                    NavGraph(
                        navController = navController,
                        viewModel = viewModel,
                        repository = repository,
                        modifier = Modifier.fillMaxSize(),
                        slavaStore = slavaStore,
                        onRescheduleSlava = { slavaReminders.update() }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.action == ACTION_OPEN_TODAY) openTodayRequests.value++
    }

    /**
     * Paints the window background the colour the app draws over it.
     *
     * `values-night/themes.xml` covers a device in dark mode, but the app's own
     * theme setting can be DARK while the system stays light, and the window
     * background is a resource the Compose theme cannot reach. Left alone it
     * stayed framework white: a white flash under the splash and a light launch
     * preview in Recents, on precisely the setting that makes the app dark.
     */
    private fun syncWindowBackground(dark: Boolean) {
        val argb = if (dark) DARK_WINDOW_BACKGROUND else LIGHT_WINDOW_BACKGROUND
        window.setBackgroundDrawable(argb.toDrawable())
    }

    companion object {
        /** Sent by the home-screen widgets: open the calendar on today's detail. */
        const val ACTION_OPEN_TODAY = "com.orthodox.calendar.OPEN_TODAY"

        /**
         * In step with `AppColors.warmBg` in ui/theme/AppColors.kt and
         * `res/values/colors.xml` — three definitions because the window, the
         * Compose theme and the resource bundle cannot share one. Change the
         * three together.
         */
        private const val LIGHT_WINDOW_BACKGROUND = 0xFFF5F3EE.toInt()
        private const val DARK_WINDOW_BACKGROUND = 0xFF1C1A17.toInt()
    }
}

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toDrawable
import androidx.core.net.toUri
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.orthodox.calendar.ui.theme.LocalIsDarkTheme
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.orthodox.calendar.ui.navigation.NavGraph
import com.orthodox.calendar.ui.screen.update.UpdateRequiredScreen
import com.orthodox.calendar.ui.theme.OrthodoxCalendarTheme
import com.orthodox.calendar.ui.viewmodel.CalendarViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as OrthodoxCalendarApp
        val repository = app.repository
        // Process-scoped: remembering the gate in the composition let a rotation
        // clear a hard update block and refetch /api/config every time.
        val updateGate = app.updateGate

        setContent {
            val viewModel: CalendarViewModel = viewModel()
            val uiState by viewModel.uiState.collectAsState()
            val navController = rememberNavController()
            val context = LocalContext.current

            // Server-controlled minimum-version gate (fail-open).
            val mustUpdate by updateGate.mustUpdate.collectAsState()
            val storeUrl by updateGate.storeUrl.collectAsState()
            LaunchedEffect(Unit) { updateGate.check() }

            // Re-read the day whenever the activity comes back. The ViewModel's
            // midnight tick is a coroutine delay, and that runs on uptime, which
            // stops in deep sleep: a phone left overnight with the app open woke
            // the tick hours late and showed yesterday as today. Coming back to
            // the app is when that must be right, so the date is re-checked here
            // and the tick re-armed from the wall clock.
            LifecycleStartEffect(viewModel) {
                viewModel.refreshToday()
                onStopOrDispose { }
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
                    NavGraph(
                        navController = navController,
                        viewModel = viewModel,
                        repository = repository,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
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

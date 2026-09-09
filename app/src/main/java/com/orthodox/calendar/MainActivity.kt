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
import androidx.core.net.toUri
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
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

            OrthodoxCalendarTheme(appTheme = uiState.theme) {
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
}

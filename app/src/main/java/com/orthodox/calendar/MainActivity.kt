package com.orthodox.calendar

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.orthodox.calendar.app.AppUpdateGate
import com.orthodox.calendar.data.repository.CalendarRepository
import com.orthodox.calendar.ui.navigation.NavGraph
import com.orthodox.calendar.ui.screen.update.UpdateRequiredScreen
import com.orthodox.calendar.ui.theme.OrthodoxCalendarTheme
import com.orthodox.calendar.ui.viewmodel.CalendarViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val repository = CalendarRepository(this)

        setContent {
            val viewModel: CalendarViewModel = viewModel()
            val uiState by viewModel.uiState.collectAsState()
            val navController = rememberNavController()
            val context = LocalContext.current

            // Server-controlled minimum-version gate (fail-open).
            val updateGate = remember { AppUpdateGate() }
            val mustUpdate by updateGate.mustUpdate.collectAsState()
            LaunchedEffect(Unit) { updateGate.check() }

            OrthodoxCalendarTheme(appTheme = uiState.theme) {
                if (mustUpdate) {
                    UpdateRequiredScreen(
                        storeUrl = updateGate.storeUrl,
                        localization = uiState.localization,
                        language = uiState.language,
                        onUpdate = {
                            updateGate.storeUrl?.let { url ->
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
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

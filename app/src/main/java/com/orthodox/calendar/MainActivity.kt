package com.orthodox.calendar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.orthodox.calendar.data.repository.CalendarRepository
import com.orthodox.calendar.ui.navigation.NavGraph
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

            OrthodoxCalendarTheme(appTheme = uiState.theme) {
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

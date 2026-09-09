package com.orthodox.calendar.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.navArgument
import com.orthodox.calendar.ui.theme.AppColors
import com.orthodox.calendar.data.repository.CalendarRepository
import com.orthodox.calendar.ui.screen.about.AboutScreen
import com.orthodox.calendar.ui.screen.detail.DayDetailScreen
import com.orthodox.calendar.ui.screen.reminder.AddReminderScreen
import com.orthodox.calendar.ui.screen.search.SaintSearchScreen
import com.orthodox.calendar.ui.screen.settings.SettingsScreen
import com.orthodox.calendar.ui.screen.splash.SplashScreen
import com.orthodox.calendar.ui.screens.CalendarTabScreen
import com.orthodox.calendar.ui.viewmodel.CalendarViewModel
import java.util.Locale

@Composable
fun NavGraph(
    navController: NavHostController,
    viewModel: CalendarViewModel,
    repository: CalendarRepository,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val localization = uiState.localization

    NavHost(
        navController = navController,
        startDestination = Routes.Splash.route,
        modifier = modifier
    ) {
        composable(Routes.Splash.route) {
            SplashScreen(
                language = uiState.language,
                onFinished = {
                    navController.navigate(Routes.Calendar.route) {
                        popUpTo(Routes.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.Calendar.route) {
            CalendarTabScreen(
                viewModel = viewModel,
                onDayClick = { day ->
                    navController.navigate(Routes.DayDetail.createRoute(day.gregorianDate))
                },
                onDateClick = { date ->
                    navController.navigate(Routes.DayDetail.createRoute(date))
                },
                onSearchClick = {
                    navController.navigate(Routes.Search.route)
                },
                onSettingsClick = {
                    navController.navigate(Routes.Settings.route)
                }
            )
        }

        composable(
            route = Routes.DayDetail.route,
            arguments = listOf(navArgument("gregorianDate") { type = NavType.StringType })
        ) { backStackEntry ->
            val gregorianDate = backStackEntry.arguments?.getString("gregorianDate") ?: return@composable
            // Resolving from daysInMonth alone rendered *nothing* — no scaffold,
            // no back button — whenever the target month had not finished
            // loading, which is exactly the case when arriving from search or
            // the date picker. Ask the repository, and show a way back meanwhile.
            var day by remember(gregorianDate) {
                mutableStateOf(uiState.daysInMonth.firstOrNull { it.gregorianDate == gregorianDate })
            }
            LaunchedEffect(gregorianDate) {
                if (day == null) day = viewModel.dayFor(gregorianDate)
            }

            val resolved = day
            if (resolved == null || localization == null) {
                DayDetailPlaceholder(onBack = { navController.popBackStack() })
            } else {
                DayDetailScreen(
                    day = resolved,
                    localization = localization,
                    language = uiState.language,
                    bibleTranslation = uiState.bibleTranslation,
                    periodInfo = uiState.fastingPeriods[gregorianDate],
                    onBack = { navController.popBackStack() },
                    onAddReminder = {
                        navController.navigate("reminder/$gregorianDate")
                    }
                )
            }
        }

        composable(
            route = "reminder/{gregorianDate}",
            arguments = listOf(navArgument("gregorianDate") { type = NavType.StringType })
        ) { backStackEntry ->
            val gregorianDate = backStackEntry.arguments?.getString("gregorianDate") ?: return@composable
            // Resolving from daysInMonth alone rendered *nothing* — no scaffold,
            // no back button — whenever the target month had not finished
            // loading, which is exactly the case when arriving from search or
            // the date picker. Ask the repository, and show a way back meanwhile.
            var day by remember(gregorianDate) {
                mutableStateOf(uiState.daysInMonth.firstOrNull { it.gregorianDate == gregorianDate })
            }
            LaunchedEffect(gregorianDate) {
                if (day == null) day = viewModel.dayFor(gregorianDate)
            }

            val resolved = day
            if (resolved == null || localization == null) {
                DayDetailPlaceholder(onBack = { navController.popBackStack() })
            } else {
                AddReminderScreen(
                    day = resolved,
                    localization = localization,
                    language = uiState.language,
                    onBack = { navController.popBackStack() }
                )
            }
        }

        composable(Routes.Search.route) {
            if (localization != null) {
                SaintSearchScreen(
                    repository = repository,
                    localization = localization,
                    language = uiState.language,
                    currentYear = uiState.currentYear,
                    onNavigateToDate = { month, day ->
                        viewModel.goToMonth(month, uiState.currentYear)
                        navController.popBackStack()
                        val dateStr = String.format(
                            Locale.ROOT, "%04d-%02d-%02d", uiState.currentYear, month, day)
                        navController.navigate(Routes.DayDetail.createRoute(dateStr))
                    },
                    onBack = { navController.popBackStack() }
                )
            }
        }

        composable(Routes.Settings.route) {
            if (localization != null) {
                SettingsScreen(
                    language = uiState.language,
                    theme = uiState.theme,
                    bibleTranslation = uiState.bibleTranslation,
                    localization = localization,
                    onLanguageChanged = { lang -> viewModel.forceReload(lang) },
                    onThemeChanged = { theme -> viewModel.setTheme(theme) },
                    onBibleTranslationChanged = { t -> viewModel.setBibleTranslation(t) },
                    onAboutClick = { navController.navigate(Routes.About.route) },
                    onBack = { navController.popBackStack() }
                )
            }
        }

        composable(Routes.About.route) {
            AboutScreen(
                language = uiState.language,
                onBack = { navController.popBackStack() }
            )
        }
    }
}


/** Shown while a day is still being resolved, or when it cannot be. Its only job
 *  is to never leave the user on a blank screen with no way back. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DayDetailPlaceholder(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = AppColors.crimson)
        }
    }
}

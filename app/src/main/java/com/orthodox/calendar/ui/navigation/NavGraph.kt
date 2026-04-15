package com.orthodox.calendar.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.orthodox.calendar.data.repository.CalendarRepository
import com.orthodox.calendar.ui.screen.about.AboutScreen
import com.orthodox.calendar.ui.screen.detail.DayDetailScreen
import com.orthodox.calendar.ui.screen.reminder.AddReminderScreen
import com.orthodox.calendar.ui.screen.search.SaintSearchScreen
import com.orthodox.calendar.ui.screen.settings.SettingsScreen
import com.orthodox.calendar.ui.screen.splash.SplashScreen
import com.orthodox.calendar.ui.screens.CalendarTabScreen
import com.orthodox.calendar.ui.viewmodel.CalendarViewModel

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
            val day = uiState.daysInMonth.firstOrNull { it.gregorianDate == gregorianDate }

            if (day != null && localization != null) {
                DayDetailScreen(
                    day = day,
                    localization = localization,
                    language = uiState.language,
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
            val day = uiState.daysInMonth.firstOrNull { it.gregorianDate == gregorianDate }

            if (day != null && localization != null) {
                AddReminderScreen(
                    day = day,
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
                        // After popping back, navigate to detail for that day
                        val dateStr = String.format("%04d-%02d-%02d", uiState.currentYear, month, day)
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
                    localization = localization,
                    onLanguageChanged = { lang -> viewModel.forceReload(lang) },
                    onThemeChanged = { theme -> viewModel.setTheme(theme) },
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

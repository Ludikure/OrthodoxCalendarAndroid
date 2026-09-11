package com.orthodox.calendar.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.orthodox.calendar.data.model.AppLanguage
import com.orthodox.calendar.ui.theme.AppColors
import com.orthodox.calendar.ui.components.CalendarLoadFailureView
import com.orthodox.calendar.ui.components.defaultNoDataMessage
import com.orthodox.calendar.ui.components.defaultOfflineMessage
import com.orthodox.calendar.ui.components.defaultRetryLabel
import com.orthodox.calendar.ui.util.backLabel
import com.orthodox.calendar.ui.util.parseIsoDate
import com.orthodox.calendar.data.repository.CalendarRepository
import com.orthodox.calendar.ui.screen.about.AboutScreen
import com.orthodox.calendar.ui.screen.detail.DayDetailScreen
import com.orthodox.calendar.ui.screen.reminder.AddReminderScreen
import com.orthodox.calendar.ui.screen.search.SaintSearchScreen
import com.orthodox.calendar.ui.screen.settings.SettingsScreen
import com.orthodox.calendar.ui.screen.splash.SplashScreen
import com.orthodox.calendar.ui.screens.CalendarTabScreen
import com.orthodox.calendar.ui.viewmodel.CalendarUiState
import com.orthodox.calendar.ui.viewmodel.CalendarViewModel
import com.orthodox.calendar.ui.viewmodel.DayOutcome
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
            var attempt by remember(gregorianDate) { mutableIntStateOf(0) }
            when (val outcome = dayOutcome(viewModel, gregorianDate, attempt, uiState)) {
                is DayOutcome.Loading -> DayDetailPlaceholder(
                    language = uiState.language,
                    onBack = { navController.popBackStack() }
                )
                is DayOutcome.Missing -> DayDetailUnresolved(
                    language = uiState.language,
                    gregorianDate = gregorianDate,
                    offline = outcome is DayOutcome.Missing.Offline,
                    onBack = { navController.popBackStack() },
                    onRetry = { attempt++ }
                )
                is DayOutcome.Found -> {
                    if (localization == null) {
                        // The bundle is on its way; this is the one case where a
                        // spinner is the honest answer.
                        DayDetailPlaceholder(
                            language = uiState.language,
                            onBack = { navController.popBackStack() }
                        )
                    } else {
                        DayDetailScreen(
                            day = outcome.day,
                            localization = localization,
                            language = uiState.language,
                            bibleTranslation = uiState.bibleTranslation,
                            periodInfo = outcome.periodInfo,
                            onBack = { navController.popBackStack() },
                            onAddReminder = {
                                navController.navigate("reminder/$gregorianDate")
                            }
                        )
                    }
                }
            }
        }

        composable(
            route = "reminder/{gregorianDate}",
            arguments = listOf(navArgument("gregorianDate") { type = NavType.StringType })
        ) { backStackEntry ->
            val gregorianDate = backStackEntry.arguments?.getString("gregorianDate") ?: return@composable
            var attempt by remember(gregorianDate) { mutableIntStateOf(0) }
            when (val outcome = dayOutcome(viewModel, gregorianDate, attempt, uiState)) {
                is DayOutcome.Loading -> DayDetailPlaceholder(
                    language = uiState.language,
                    onBack = { navController.popBackStack() }
                )
                is DayOutcome.Missing -> DayDetailUnresolved(
                    language = uiState.language,
                    gregorianDate = gregorianDate,
                    offline = outcome is DayOutcome.Missing.Offline,
                    onBack = { navController.popBackStack() },
                    onRetry = { attempt++ }
                )
                is DayOutcome.Found -> {
                    if (localization == null) {
                        DayDetailPlaceholder(
                            language = uiState.language,
                            onBack = { navController.popBackStack() }
                        )
                    } else {
                        AddReminderScreen(
                            day = outcome.day,
                            localization = localization,
                            language = uiState.language,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
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


/**
 * Resolves one date into a day through the ViewModel, re-running on [attempt].
 *
 * The routes used to resolve from `uiState.daysInMonth` alone, which rendered
 * *nothing* — no scaffold, no back button — whenever the target month had not
 * finished loading: exactly the case when arriving from search or the date
 * picker. Asking the ViewModel fixed the "nothing"; returning a plain nullable
 * day then turned "this year cannot be loaded" into an endless spinner, because
 * "not yet" and "never" were the same value. The outcome says which.
 *
 * The first frame resolves from the days already on screen, so tapping a day in
 * the list opens it without a spinner flash.
 */
@Composable
private fun dayOutcome(
    viewModel: CalendarViewModel,
    gregorianDate: String,
    attempt: Int,
    uiState: CalendarUiState
): DayOutcome {
    var outcome by remember(gregorianDate, attempt) {
        mutableStateOf<DayOutcome>(
            uiState.daysInMonth
                .firstOrNull { it.gregorianDate == gregorianDate }
                ?.let { DayOutcome.Found(it, uiState.fastingPeriods[gregorianDate]) }
                ?: DayOutcome.Loading
        )
    }
    LaunchedEffect(gregorianDate, attempt) {
        outcome = viewModel.resolveDay(gregorianDate)
    }
    return outcome
}

/** Shown while a day is still being resolved. Its only job is to never leave the
 *  user on a blank screen with no way back. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DayDetailPlaceholder(language: AppLanguage, onBack: () -> Unit) {
    Scaffold(
        topBar = { DayDetailTopBar(onBack = onBack, language = language) }
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = AppColors.crimson)
        }
    }
}

/**
 * The day could not be resolved — the archive does not cover that year, or the
 * data could not be fetched. Before this existed the route treated it the same
 * as "still loading": a spinner with no retry and no explanation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DayDetailUnresolved(
    language: AppLanguage,
    gregorianDate: String,
    offline: Boolean,
    onBack: () -> Unit,
    onRetry: () -> Unit
) {
    Scaffold(
        topBar = { DayDetailTopBar(onBack = onBack, language = language) }
    ) { padding ->
        val year = parseIsoDate(gregorianDate)?.year
        CalendarLoadFailureView(
            message = if (offline || year == null) defaultOfflineMessage(language)
            else defaultNoDataMessage(language, year),
            retryLabel = defaultRetryLabel(language),
            onRetry = onRetry,
            modifier = Modifier.padding(padding)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DayDetailTopBar(onBack: () -> Unit, language: AppLanguage) =
    TopAppBar(
        title = {},
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = backLabel(language))
            }
        }
    )

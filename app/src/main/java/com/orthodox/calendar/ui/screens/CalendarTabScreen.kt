package com.orthodox.calendar.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.platform.LocalView
import com.orthodox.calendar.ui.util.Haptics
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orthodox.calendar.ui.components.CalendarTitle
import com.orthodox.calendar.ui.components.FastingPeriodBanner
import com.orthodox.calendar.ui.components.MonthHeaderBar
import com.orthodox.calendar.ui.components.MonthListScreen
import com.orthodox.calendar.ui.screen.datepicker.DatePickerSheet
import com.orthodox.calendar.ui.screen.grid.CalendarGridScreen
import com.orthodox.calendar.ui.theme.AppColors
import java.util.Locale
import com.orthodox.calendar.ui.viewmodel.CalendarViewModel
import com.orthodox.calendar.ui.viewmodel.ViewMode
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarTabScreen(
    viewModel: CalendarViewModel,
    onDayClick: (com.orthodox.calendar.data.model.CalendarDay) -> Unit = {},
    onDateClick: (String) -> Unit = {},
    onSearchClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val localization = uiState.localization ?: return
    val view = LocalView.current

    var showDatePicker by remember { mutableStateOf(false) }
    // skipPartiallyExpanded so the sheet opens fully and the Today button at the
    // bottom is visible without dragging it up.
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.warmBg)
            .statusBarsPadding()
    ) {
        // Toolbar row: Today | title area | Search + Settings
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppColors.warmBg)
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = localization.ui.todayLabel,
                fontSize = 14.sp,
                color = AppColors.mutedText,
                modifier = Modifier.clickable { Haptics.medium(view); viewModel.goToToday() }
            )

            Spacer(modifier = Modifier.weight(1f))

            // Real icon buttons: an emoji in a Text has no button role, no label
            // for TalkBack, and renders differently on every vendor's font.
            IconButton(onClick = { Haptics.light(view); onSearchClick() }) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = defaultSearchLabel(uiState.language),
                    tint = AppColors.mutedText
                )
            }
            IconButton(onClick = { Haptics.light(view); onSettingsClick() }) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = localization.ui.settingsLabel,
                    tint = AppColors.mutedText
                )
            }
        }

        CalendarTitle(
            appTitle = localization.ui.appTitle,
            language = uiState.language
        )

        MonthHeaderBar(
            currentMonth = uiState.currentMonth,
            currentYear = uiState.currentYear,
            viewMode = uiState.viewMode,
            monthName = localization.ui.months.getOrElse(uiState.currentMonth - 1) { "" },
            onPreviousMonth = { viewModel.goToPreviousMonth() },
            onNextMonth = { viewModel.goToNextMonth() },
            onViewModeChange = { viewModel.setViewMode(it) },
            onMonthTap = { showDatePicker = true }
        )

        // Fasting season banner (Great Lent, etc.). When today is in the viewed
        // month the banner reflects *today's* status: its season if we're in one,
        // otherwise nothing — we must not fall back to a fast that has already
        // ended (or not yet begun) elsewhere in the month, which would show e.g.
        // "Day 24 of 34" of the Apostles' Fast days after it ended. When browsing
        // another month we show that month's season as an overview (name + range,
        // no day index — there is no "current day" there).
        val today = java.time.LocalDate.now().toString()
        val todayInView = uiState.daysInMonth.any { it.gregorianDate == today }
        val focalDate = if (todayInView) {
            today.takeIf { uiState.fastingPeriods.containsKey(it) }
        } else {
            uiState.daysInMonth.firstOrNull { uiState.fastingPeriods.containsKey(it.gregorianDate) }
                ?.gregorianDate
        }
        focalDate?.let { uiState.fastingPeriods[it] }?.let { period ->
            FastingPeriodBanner(
                period = period,
                localization = localization,
                language = uiState.language,
                // `complete` is false when the run touches the edge of the loaded
                // data — a fast crossing a year boundary looks like two short
                // ones there, so its day index would be wrong rather than absent.
                showsDayIndex = todayInView && period.complete
            )
        }

        // Calendar content - switch on view mode, or show loading/offline/error
        if (uiState.isLoading && uiState.daysInMonth.isEmpty()) {
            // A downloaded year takes a second or so; without this the screen
            // is simply empty and reads as a hang.
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(color = AppColors.crimson)
                localization.ui.loadingLabel?.let {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = it, fontSize = 13.sp, color = AppColors.mutedText)
                }
            }
        } else if ((uiState.isOffline || uiState.errorMessage != null) && uiState.daysInMonth.isEmpty()) {
            // Retrying only helps when the load failed on the network; blaming
            // the connection for a year that simply has no data sends the user
            // chasing wifi.
            CalendarLoadFailureView(
                message = if (uiState.isOffline) {
                    localization.ui.offlineMessage ?: defaultOfflineMessage(uiState.language)
                } else {
                    defaultNoDataMessage(uiState.language, uiState.currentYear)
                },
                retryLabel = localization.ui.retryLabel
                    ?: defaultRetryLabel(uiState.language),
                onRetry = { Haptics.medium(view); viewModel.retry() }
            )
        } else {
            when (uiState.viewMode) {
                ViewMode.LIST -> {
                    MonthListScreen(
                        days = uiState.daysInMonth,
                        localization = localization,
                        language = uiState.language,
                        loadedLocale = uiState.loadedLocale,
                        scrollToTodayTrigger = uiState.scrollToTodayTrigger,
                        onDayClick = onDayClick
                    )
                }
                ViewMode.GRID -> {
                    CalendarGridScreen(
                        days = uiState.daysInMonth,
                        localization = localization,
                        language = uiState.language,
                        loadedLocale = uiState.loadedLocale,
                        onDayClick = onDayClick
                    )
                }
            }
        }
    }

    // Date picker bottom sheet
    if (showDatePicker) {
        ModalBottomSheet(
            onDismissRequest = { showDatePicker = false },
            sheetState = sheetState
        ) {
            DatePickerSheet(
                currentMonth = uiState.currentMonth,
                currentYear = uiState.currentYear,
                localization = localization,
                language = uiState.language,
                onMonthSelected = { month, year ->
                    viewModel.goToMonth(month, year)
                },
                onDaySelected = { month, year, day ->
                    // Move the calendar behind the sheet *and* open the day —
                    // this used to discard `day` and behave like a month tap.
                    viewModel.goToMonth(month, year)
                    onDateClick(String.format(Locale.ROOT, "%04d-%02d-%02d", year, month, day))
                },
                onTodayClick = { viewModel.goToToday() },
                onDismiss = {
                    scope.launch {
                        sheetState.hide()
                        showDatePicker = false
                    }
                }
            )
        }
    }
}

/** Offline/error state with a retry action. Mirror of iOS CalendarLoadFailureView. */
@Composable
private fun CalendarLoadFailureView(
    message: String,
    retryLabel: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "⛪", fontSize = 40.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            fontSize = 16.sp,
            color = AppColors.bodyText,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = retryLabel,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.warmBg,
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(AppColors.crimson)
                .clickable { onRetry() }
                .padding(horizontal = 24.dp, vertical = 10.dp)
        )
    }
}

private fun defaultOfflineMessage(language: com.orthodox.calendar.data.model.AppLanguage): String =
    when (language) {
        com.orthodox.calendar.data.model.AppLanguage.SR ->
            "Немогуће учитавање података. Проверите везу."
        com.orthodox.calendar.data.model.AppLanguage.RU ->
            "Не удалось загрузить данные. Проверьте соединение."
        com.orthodox.calendar.data.model.AppLanguage.EN,
        com.orthodox.calendar.data.model.AppLanguage.EN_NC ->
            "Couldn't load data. Check your connection."
    }

/** A year the archive does not cover, as opposed to one that failed to download.
 *  Kept here rather than in the shared `ui` strings so the two apps'
 *  localization bundles stay byte-identical. */
private fun defaultNoDataMessage(language: com.orthodox.calendar.data.model.AppLanguage, year: Int): String =
    when (language) {
        com.orthodox.calendar.data.model.AppLanguage.SR -> "Нема података за $year. годину."
        com.orthodox.calendar.data.model.AppLanguage.RU -> "Нет данных за $year год."
        com.orthodox.calendar.data.model.AppLanguage.EN,
        com.orthodox.calendar.data.model.AppLanguage.EN_NC -> "No calendar data for $year."
    }


private fun defaultSearchLabel(language: com.orthodox.calendar.data.model.AppLanguage): String =
    when (language) {
        com.orthodox.calendar.data.model.AppLanguage.SR -> "Претрага"
        com.orthodox.calendar.data.model.AppLanguage.RU -> "Поиск"
        com.orthodox.calendar.data.model.AppLanguage.EN,
        com.orthodox.calendar.data.model.AppLanguage.EN_NC -> "Search"
    }


private fun defaultRetryLabel(language: com.orthodox.calendar.data.model.AppLanguage): String =
    when (language) {
        com.orthodox.calendar.data.model.AppLanguage.SR -> "Покушај поново"
        com.orthodox.calendar.data.model.AppLanguage.RU -> "Повторить"
        com.orthodox.calendar.data.model.AppLanguage.EN,
        com.orthodox.calendar.data.model.AppLanguage.EN_NC -> "Retry"
    }

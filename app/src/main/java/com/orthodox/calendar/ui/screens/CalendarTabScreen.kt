package com.orthodox.calendar.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.platform.LocalView
import com.orthodox.calendar.ui.util.Haptics
import androidx.compose.foundation.layout.Arrangement
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
import com.orthodox.calendar.ui.viewmodel.CalendarViewModel
import com.orthodox.calendar.ui.viewmodel.ViewMode
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarTabScreen(
    viewModel: CalendarViewModel,
    onDayClick: (com.orthodox.calendar.data.model.CalendarDay) -> Unit = {},
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

            // Search icon
            Text(
                text = "\uD83D\uDD0D",
                fontSize = 18.sp,
                modifier = Modifier
                    .clickable { Haptics.light(view); onSearchClick() }
                    .padding(horizontal = 8.dp)
            )

            // Settings icon
            Text(
                text = "\u2699",
                fontSize = 20.sp,
                color = AppColors.mutedText,
                modifier = Modifier
                    .clickable { Haptics.light(view); onSettingsClick() }
                    .padding(horizontal = 8.dp)
            )
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

        // Fasting season banner (Great Lent, etc.) when the viewed month touches a
        // season. Focal day = today if it's in view, otherwise the first in-season day.
        val today = java.time.LocalDate.now().toString()
        val focalDate = if (uiState.fastingPeriods.containsKey(today) &&
            uiState.daysInMonth.any { it.gregorianDate == today }
        ) {
            today
        } else {
            uiState.daysInMonth.firstOrNull { uiState.fastingPeriods.containsKey(it.gregorianDate) }
                ?.gregorianDate
        }
        focalDate?.let { uiState.fastingPeriods[it] }?.let { period ->
            FastingPeriodBanner(
                period = period,
                localization = localization,
                language = uiState.language
            )
        }

        // Calendar content - switch on view mode, or show offline/error state
        if (uiState.isOffline && uiState.daysInMonth.isEmpty()) {
            CalendarLoadFailureView(
                message = localization.ui.offlineMessage
                    ?: defaultOfflineMessage(uiState.language),
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
                    viewModel.goToMonth(month, year)
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

private fun defaultRetryLabel(language: com.orthodox.calendar.data.model.AppLanguage): String =
    when (language) {
        com.orthodox.calendar.data.model.AppLanguage.SR -> "Покушај поново"
        com.orthodox.calendar.data.model.AppLanguage.RU -> "Повторить"
        com.orthodox.calendar.data.model.AppLanguage.EN,
        com.orthodox.calendar.data.model.AppLanguage.EN_NC -> "Retry"
    }

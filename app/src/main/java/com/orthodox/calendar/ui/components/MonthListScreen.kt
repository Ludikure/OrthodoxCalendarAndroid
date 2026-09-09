package com.orthodox.calendar.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.orthodox.calendar.data.model.AppLanguage
import com.orthodox.calendar.data.model.CalendarDay
import com.orthodox.calendar.data.model.LocalizationBundle
import com.orthodox.calendar.ui.theme.AppColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun MonthListScreen(
    days: List<CalendarDay>,
    localization: LocalizationBundle,
    language: AppLanguage,
    loadedLocale: String,
    scrollToTodayTrigger: Boolean,
    modifier: Modifier = Modifier,
    onDayClick: (CalendarDay) -> Unit = {}
) {
    val todayString = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ROOT))
    val listState = rememberLazyListState()

    // Scroll to today when trigger changes or days load
    LaunchedEffect(scrollToTodayTrigger, days.size) {
        val todayIndex = days.indexOfFirst { it.gregorianDate == todayString }
        if (todayIndex >= 0) {
            listState.animateScrollToItem(todayIndex)
        }
    }

    LazyColumn(
        state = listState,
        // The window draws behind the navigation bar (enableEdgeToEdge), and the
        // root Column only insets the status bar — without this the last day of
        // the month sits under the gesture pill. Padding the content rather than
        // the list keeps the card background running to the bottom edge.
        contentPadding = WindowInsets.navigationBars.asPaddingValues(),
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.cardBg)
    ) {
        items(
            items = days,
            key = { "${loadedLocale}_${it.gregorianDate}" }
        ) { day ->
            Box(modifier = Modifier.clickable { onDayClick(day) }) {
                DayRowView(
                    day = day,
                    isToday = day.gregorianDate == todayString,
                    localization = localization,
                    language = language
                )
            }

            // Divider
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(AppColors.warmBorder)
            )
        }
    }
}

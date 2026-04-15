package com.orthodox.calendar.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.orthodox.calendar.data.model.CalendarDay
import com.orthodox.calendar.data.model.LocalizationBundle
import com.orthodox.calendar.ui.theme.AppColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun MonthListScreen(
    days: List<CalendarDay>,
    localization: LocalizationBundle,
    loadedLocale: String,
    scrollToTodayTrigger: Boolean,
    onDayClick: (CalendarDay) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val todayString = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
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
                    localization = localization
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

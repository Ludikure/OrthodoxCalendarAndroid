package com.orthodox.calendar.ui.screen.grid

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orthodox.calendar.data.model.AppLanguage
import com.orthodox.calendar.data.model.CalendarDay
import com.orthodox.calendar.data.model.LocalizationBundle
import com.orthodox.calendar.ui.theme.AppColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun CalendarGridScreen(
    days: List<CalendarDay>,
    localization: LocalizationBundle,
    language: AppLanguage,
    loadedLocale: String,
    onDayClick: (CalendarDay) -> Unit,
    modifier: Modifier = Modifier
) {
    val todayString = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
    var selectedDay by remember(loadedLocale, days.size) { mutableStateOf<CalendarDay?>(null) }

    // Auto-select today or first day
    LaunchedEffect(days, loadedLocale) {
        val today = days.firstOrNull { it.gregorianDate == todayString }
        selectedDay = today ?: days.firstOrNull()
    }

    val view = androidx.compose.ui.platform.LocalView.current

    // Calculate offset: dayOfWeek of first day (0=Mon..6=Sun) for Mon-first grid
    val firstDayOffset = days.firstOrNull()?.dayOfWeek ?: 0

    // Mon-first weekday headers: index 1,2,3,4,5,6,0 in the localization array
    val weekdayOrder = listOf(1, 2, 3, 4, 5, 6, 0)
    val abbrevs = localization.ui.daysOfWeek

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.warmBg)
            .verticalScroll(rememberScrollState())
    ) {
        // Weekday headers
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppColors.warmBorder)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            for (i in weekdayOrder) {
                val label = abbrevs.getOrElse(i) { "" }.take(2)
                Text(
                    text = label,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    color = if (i == 0 || i == 6) AppColors.crimson else AppColors.mutedText,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }

        // Grid - use a manual grid layout since LazyVerticalGrid doesn't work well inside ScrollView
        val totalCells = firstDayOffset + days.size
        val rows = (totalCells + 6) / 7

        for (row in 0 until rows) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .padding(vertical = 4.dp)
            ) {
                for (col in 0 until 7) {
                    val index = row * 7 + col
                    val dayIndex = index - firstDayOffset

                    if (dayIndex < 0 || dayIndex >= days.size) {
                        // Empty cell
                        Spacer(
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .padding(horizontal = 2.dp)
                        )
                    } else {
                        val day = days[dayIndex]
                        GridDayCell(
                            day = day,
                            isToday = day.gregorianDate == todayString,
                            isSelected = selectedDay?.gregorianDate == day.gregorianDate,
                            onClick = { com.orthodox.calendar.ui.util.Haptics.selection(view); selectedDay = day },
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 2.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Selected day card
        AnimatedVisibility(
            visible = selectedDay != null,
            enter = expandVertically() + fadeIn(),
            exit = fadeOut()
        ) {
            selectedDay?.let { day ->
                SelectedDayCard(
                    day = day,
                    localization = localization,
                    language = language,
                    onClick = { onDayClick(day) },
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 8.dp)
                )
            }
        }

        // Fasting legend
        FastingLegend(
            language = language,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = 20.dp)
        )
    }
}

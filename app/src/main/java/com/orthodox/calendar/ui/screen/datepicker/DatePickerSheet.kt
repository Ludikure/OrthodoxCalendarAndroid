package com.orthodox.calendar.ui.screen.datepicker

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orthodox.calendar.data.model.AppLanguage
import com.orthodox.calendar.data.model.LocalizationBundle
import com.orthodox.calendar.ui.theme.AppColors
import com.orthodox.calendar.ui.viewmodel.CalendarUiState
import java.time.LocalDate
import java.time.YearMonth

private enum class PickerMode { MONTH, DAY }

@Composable
fun DatePickerSheet(
    currentMonth: Int,
    currentYear: Int,
    localization: LocalizationBundle,
    language: AppLanguage,
    onMonthSelected: (month: Int, year: Int) -> Unit,
    onDaySelected: (month: Int, year: Int, day: Int) -> Unit,
    onTodayClick: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var mode by remember { mutableStateOf(PickerMode.MONTH) }
    var pickerYear by remember { mutableIntStateOf(currentYear) }
    var pickerMonth by remember { mutableIntStateOf(currentMonth) }

    val selectMonthLabel = when (language) {
        AppLanguage.SR -> "\u0418\u0437\u0430\u0431\u0435\u0440\u0438\u0442\u0435 \u043C\u0435\u0441\u0435\u0446"
        AppLanguage.RU -> "\u0412\u044B\u0431\u0435\u0440\u0438\u0442\u0435 \u043C\u0435\u0441\u044F\u0446"
        AppLanguage.EN, AppLanguage.EN_NC -> "Select month"
    }
    val selectDateLabel = when (language) {
        AppLanguage.SR -> "\u0418\u0437\u0430\u0431\u0435\u0440\u0438\u0442\u0435 \u0434\u0430\u0442\u0443\u043C"
        AppLanguage.RU -> "\u0412\u044B\u0431\u0435\u0440\u0438\u0442\u0435 \u0434\u0430\u0442\u0443"
        AppLanguage.EN, AppLanguage.EN_NC -> "Select date"
    }
    val todayButtonLabel = when (language) {
        AppLanguage.SR -> "\u0414\u0430\u043D\u0430\u0441"
        AppLanguage.RU -> "\u0421\u0435\u0433\u043E\u0434\u043D\u044F"
        AppLanguage.EN, AppLanguage.EN_NC -> "Today"
    }
    val exactDateLabel = when (language) {
        AppLanguage.SR -> "\u0422\u0430\u0447\u0430\u043D \u0434\u0430\u0442\u0443\u043C"
        AppLanguage.RU -> "\u0422\u043E\u0447\u043D\u0430\u044F \u0434\u0430\u0442\u0430"
        AppLanguage.EN, AppLanguage.EN_NC -> "Exact date"
    }
    val backToMonthsLabel = when (language) {
        AppLanguage.SR -> "\u041D\u0430\u0437\u0430\u0434 \u043D\u0430 \u043C\u0435\u0441\u0435\u0446\u0435"
        AppLanguage.RU -> "\u041D\u0430\u0437\u0430\u0434 \u043A \u043C\u0435\u0441\u044F\u0446\u0430\u043C"
        AppLanguage.EN, AppLanguage.EN_NC -> "Back to months"
    }

    val now = LocalDate.now()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
    ) {
        // Drag handle
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 10.dp, bottom = 6.dp)
                .size(width = 36.dp, height = 4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color.Gray.copy(alpha = 0.3f))
        )

        // Title + close
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (mode == PickerMode.MONTH) selectMonthLabel else selectDateLabel,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = onDismiss) {
                Text(text = "\u2715", fontSize = 18.sp, color = AppColors.mutedText)
            }
        }

        if (mode == PickerMode.MONTH) {
            // Year navigation
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = { if (pickerYear > CalendarUiState.MIN_YEAR) pickerYear-- },
                    enabled = pickerYear > CalendarUiState.MIN_YEAR
                ) {
                    Text(text = "\u276E", fontSize = 18.sp, color = if (pickerYear > CalendarUiState.MIN_YEAR) AppColors.darkText else AppColors.mutedText.copy(alpha = 0.3f))
                }
                Text(
                    text = pickerYear.toString(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                )
                IconButton(
                    onClick = { if (pickerYear < CalendarUiState.MAX_YEAR) pickerYear++ },
                    enabled = pickerYear < CalendarUiState.MAX_YEAR
                ) {
                    Text(text = "\u276F", fontSize = 18.sp, color = if (pickerYear < CalendarUiState.MAX_YEAR) AppColors.darkText else AppColors.mutedText.copy(alpha = 0.3f))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 4x3 month grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(280.dp)
            ) {
                items(12) { i ->
                    val month = i + 1
                    val isSelected = month == currentMonth && pickerYear == currentYear
                    val isCurrent = month == now.monthValue && pickerYear == now.year

                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.onBackground
                                else Color.Transparent
                            )
                            .then(
                                if (isCurrent && !isSelected) Modifier.border(1.5.dp, AppColors.gold, RoundedCornerShape(12.dp))
                                else Modifier
                            )
                            .clickable {
                                onMonthSelected(month, pickerYear)
                                onDismiss()
                            }
                            .padding(vertical = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = localization.ui.months.getOrElse(i) { "" },
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 14.sp,
                            color = if (isSelected) MaterialTheme.colorScheme.background
                            else MaterialTheme.colorScheme.onBackground
                        )
                        if (isCurrent && !isSelected) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 4.dp)
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(AppColors.crimson)
                            )
                        }
                    }
                }
            }

            // Exact date button
            TextButton(
                onClick = {
                    pickerMonth = currentMonth
                    mode = PickerMode.DAY
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .border(1.5.dp, Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            ) {
                Text(
                    text = exactDateLabel,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.mutedText
                )
            }
        } else {
            // Day calendar mode
            // Month/year navigation
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val atMin = pickerYear <= CalendarUiState.MIN_YEAR && pickerMonth <= 1
                IconButton(
                    onClick = {
                        if (pickerMonth == 1) { pickerMonth = 12; pickerYear-- }
                        else pickerMonth--
                    },
                    enabled = !atMin
                ) {
                    Text(text = "\u276E", fontSize = 18.sp, color = if (!atMin) AppColors.darkText else AppColors.mutedText.copy(alpha = 0.3f))
                }

                TextButton(onClick = { mode = PickerMode.MONTH }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = localization.ui.months.getOrElse(pickerMonth - 1) { "" },
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = pickerYear.toString(),
                            fontSize = 14.sp,
                            color = AppColors.mutedText
                        )
                        Text(text = " \u25B2", fontSize = 10.sp, color = AppColors.mutedText)
                    }
                }

                val atMax = pickerYear >= CalendarUiState.MAX_YEAR && pickerMonth >= 12
                IconButton(
                    onClick = {
                        if (pickerMonth == 12) { pickerMonth = 1; pickerYear++ }
                        else pickerMonth++
                    },
                    enabled = !atMax
                ) {
                    Text(text = "\u276F", fontSize = 18.sp, color = if (!atMax) AppColors.darkText else AppColors.mutedText.copy(alpha = 0.3f))
                }
            }

            // Weekday headers
            val weekdayOrder = listOf(1, 2, 3, 4, 5, 6, 0)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                for (i in weekdayOrder) {
                    Text(
                        text = localization.ui.daysOfWeek.getOrElse(i) { "" }.take(2),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (i == 0 || i == 6) AppColors.crimson else AppColors.mutedText,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Day grid
            val yearMonth = YearMonth.of(pickerYear, pickerMonth)
            val numDays = yearMonth.lengthOfMonth()
            val firstDayOfWeek = yearMonth.atDay(1).dayOfWeek.value // 1=Mon..7=Sun
            val offset = firstDayOfWeek - 1 // Mon=0

            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(260.dp)
            ) {
                // Empty cells
                items(offset) {
                    Spacer(modifier = Modifier.height(40.dp))
                }

                // Day cells
                items(numDays) { i ->
                    val day = i + 1
                    val isToday = day == now.dayOfMonth && pickerMonth == now.monthValue && pickerYear == now.year
                    val cellWeekday = (offset + i) % 7
                    val isSunday = cellWeekday == 6
                    val isSaturday = cellWeekday == 5

                    Box(
                        modifier = Modifier
                            .height(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isToday) MaterialTheme.colorScheme.onBackground
                                else Color.Transparent
                            )
                            .clickable {
                                onDaySelected(pickerMonth, pickerYear, day)
                                onDismiss()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = day.toString(),
                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 14.sp,
                            color = when {
                                isToday -> MaterialTheme.colorScheme.background
                                isSunday || isSaturday -> AppColors.crimson
                                else -> MaterialTheme.colorScheme.onBackground
                            }
                        )
                    }
                }
            }

            // Back to months
            TextButton(
                onClick = { mode = PickerMode.MONTH },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .border(1.5.dp, Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            ) {
                Text(
                    text = backToMonthsLabel,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.mutedText
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Today button
        Button(
            onClick = {
                onTodayClick()
                onDismiss()
            },
            colors = ButtonDefaults.buttonColors(containerColor = AppColors.crimson),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            Text(
                text = todayButtonLabel,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(vertical = 6.dp)
            )
        }
    }
}

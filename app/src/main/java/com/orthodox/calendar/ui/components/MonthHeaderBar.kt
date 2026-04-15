package com.orthodox.calendar.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.platform.LocalView
import com.orthodox.calendar.ui.util.Haptics
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orthodox.calendar.ui.theme.AppColors
import com.orthodox.calendar.ui.viewmodel.ViewMode

private val headerColor = Color(0xFF7A1B1B)

@Composable
fun MonthHeaderBar(
    currentMonth: Int,
    currentYear: Int,
    viewMode: ViewMode,
    monthName: String,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onViewModeChange: (ViewMode) -> Unit,
    onMonthTap: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(headerColor)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val view = LocalView.current
        // Previous month
        IconButton(onClick = { Haptics.selection(view); onPreviousMonth() }, modifier = Modifier.size(40.dp)) {
            Text(
                text = "\u276E",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Month name + year (tappable for date picker)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { Haptics.light(view); onMonthTap() }
        ) {
            Text(
                text = monthName,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = Color.White
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = currentYear.toString(),
                fontSize = 15.sp,
                color = Color.White.copy(alpha = 0.7f)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // View mode toggle
        Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.padding(end = 4.dp)
        ) {
            ViewModeButton(
                label = "\u2630",
                isSelected = viewMode == ViewMode.LIST,
                onClick = { Haptics.selection(view); onViewModeChange(ViewMode.LIST) }
            )
            ViewModeButton(
                label = "\u25A6",
                isSelected = viewMode == ViewMode.GRID,
                onClick = { Haptics.selection(view); onViewModeChange(ViewMode.GRID) }
            )
        }

        // Next month
        IconButton(onClick = { Haptics.selection(view); onNextMonth() }, modifier = Modifier.size(40.dp)) {
            Text(
                text = "\u276F",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun ViewModeButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgColor = if (isSelected) AppColors.goldAccent else Color.Transparent
    val textColor = if (isSelected) Color(0xFF2C2418) else Color.White.copy(alpha = 0.5f)

    Text(
        text = label,
        color = textColor,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    )
}

package com.orthodox.calendar.ui.screen.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orthodox.calendar.data.nameday.DayName
import com.orthodox.calendar.data.nameday.NameDayCatalog
import com.orthodox.calendar.data.nameday.NameDayChoice
import com.orthodox.calendar.data.nameday.NameDayMark
import com.orthodox.calendar.ui.components.NameDayIcon
import com.orthodox.calendar.ui.theme.AppColors
import com.orthodox.calendar.ui.util.Haptics

/*
 * The name-day parts of a day's detail (Russian only). Mirror of
 * `nameDayCard` and `nameDaysSection` in `OrthodoxCalendar/Views/DayDetailView.swift`.
 */

/** The user's own name day on this day, and one line per friend's. */
@Composable
internal fun NameDayCard(mark: NameDayMark, mine: NameDayChoice?, isToday: Boolean) {
    Column {
        if (mark.isMine && mine != null) {
            val heading = if (isToday) "С днём ангела!" else mine.churchName
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppColors.bannerBg, RoundedCornerShape(16.dp))
                    .padding(18.dp)
                    .clearAndSetSemantics { contentDescription = "Ваши именины. $heading. ${mine.title}" }
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(AppColors.cardBg)
                        .border(2.dp, AppColors.gold, CircleShape)
                ) {
                    NameDayIcon(Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "ВАШИ ИМЕНИНЫ",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    color = AppColors.slavaGold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = heading,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = AppColors.bannerTitle,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = mine.title,
                    fontFamily = FontFamily.Serif,
                    fontSize = 15.sp,
                    color = AppColors.bodyText,
                    textAlign = TextAlign.Center
                )
            }
        }
        mark.friendLines.forEachIndexed { index, line ->
            if (index > 0 || mark.isMine) Spacer(modifier = Modifier.height(10.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(AppColors.slavaRowBg, RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                NameDayIcon(Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Именины: $line",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.slavaGold
                )
            }
        }
    }
}

/**
 * "Именины": the day's names, main saints first, the first dozen and an
 * "и другие" that opens the rest. Main saints are bold; the names the user and
 * their friends keep are gold.
 */
@Composable
internal fun NameDaysSection(names: List<DayName>, highlighted: Set<String>) {
    val view = LocalView.current
    var showAll by remember(names) { mutableStateOf(false) }
    val (cappedList, hidden) = NameDayCatalog.capped(names)
    val shown = if (showAll) names else cappedList
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            NameDayIcon(Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Именины",
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = AppColors.darkText
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        val gold = AppColors.slavaGold
        Text(
            text = buildAnnotatedString {
                shown.forEachIndexed { i, n ->
                    if (i > 0) append(", ")
                    val style = SpanStyle(
                        fontWeight = if (n.isMain) FontWeight.Bold else null,
                        color = if (n.name in highlighted) gold else androidx.compose.ui.graphics.Color.Unspecified
                    )
                    withStyle(style) { append(n.name) }
                }
            },
            fontFamily = FontFamily.Serif,
            fontSize = 15.sp,
            lineHeight = 22.sp,
            color = AppColors.bodyText,
            modifier = Modifier
                .fillMaxWidth()
                .background(AppColors.cardBg, RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp)
        )
        if (hidden > 0) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = if (showAll) "Свернуть" else "и другие ($hidden)",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.crimson,
                modifier = Modifier
                    .clickable { Haptics.light(view); showAll = !showAll }
                    .padding(start = 2.dp, top = 4.dp, bottom = 4.dp, end = 8.dp)
            )
        }
    }
}

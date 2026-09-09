package com.orthodox.calendar.ui.screen.detail

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orthodox.calendar.data.model.AppLanguage
import com.orthodox.calendar.data.model.BibleTranslation
import com.orthodox.calendar.data.model.ScriptureReading
import com.orthodox.calendar.ui.theme.AppColors
import com.orthodox.calendar.ui.util.readingTypeLabel
import com.orthodox.calendar.ui.util.serviceLabel

@Composable
fun ReadingCard(
    reading: ScriptureReading,
    language: AppLanguage,
    bibleTranslation: BibleTranslation,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    // NT readings honour the KJV/WEB choice (English only); OT and other locales
    // fall back to the bundled text. Mirror of iOS ReadingCard.displayText.
    val displayText = reading.text(bibleTranslation)

    // null for a type outside the three known ones — the Great Canon's odes come
    // through as "other", and the card used to print the literal word OTHER
    // above a title that already reads as one ("Песма прва").
    val localizedType = readingTypeLabel(reading.type, language)
    // `service` is set on a fraction of readings; `source` covers the rest.
    val service = serviceLabel(reading, language)

    val zachaloLabel = when (language) {
        AppLanguage.SR, AppLanguage.RU -> "\u0437\u0430\u0447."
        AppLanguage.EN, AppLanguage.EN_NC -> "ch."
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 1.dp, shape = RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(AppColors.cardBg)
            .border(1.5.dp, AppColors.warmBorder, RoundedCornerShape(12.dp))
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        // Header row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    if (displayText != null) {
                        isExpanded = !isExpanded
                    }
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Service label
            service?.let {
                Text(
                    text = it,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    color = AppColors.mutedText
                )
                Spacer(modifier = Modifier.width(6.dp))
            }

            localizedType?.let {
                Text(
                    text = it,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                    color = AppColors.mutedText
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Reference badge
            Text(
                text = reading.displayReference,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = AppColors.darkText,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(AppColors.warmBorder)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            )

            // Expand chevron
            if (displayText != null) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isExpanded) "\u25B2" else "\u25BC",
                    fontSize = 10.sp,
                    color = AppColors.lightMuted
                )
            }
        }

        // Zachalo
        reading.zachalo?.let { zachalo ->
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$zachaloLabel $zachalo",
                fontSize = 12.sp,
                color = AppColors.mutedText
            )
        }

        // Expandable scripture text
        if (isExpanded && !displayText.isNullOrEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = displayText,
                fontFamily = FontFamily.Serif,
                fontSize = 14.sp,
                color = AppColors.bodyText,
                lineHeight = 22.sp
            )
        }
    }
}

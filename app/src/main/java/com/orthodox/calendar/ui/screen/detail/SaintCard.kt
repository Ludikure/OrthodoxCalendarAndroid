package com.orthodox.calendar.ui.screen.detail

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orthodox.calendar.data.model.Feast
import com.orthodox.calendar.data.model.SaintBio
import com.orthodox.calendar.ui.theme.AppColors

@Composable
fun SaintCard(
    feast: Feast,
    bio: SaintBio?,
    localizedType: String,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    // Feast description takes priority over bio
    val expandableText = feast.description?.takeIf { it.isNotEmpty() } ?: bio?.text

    val goldAccent = AppColors.goldAccent

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(AppColors.cardBg)
            .drawBehind {
                // Left gold accent bar
                drawRect(
                    color = goldAccent,
                    topLeft = Offset.Zero,
                    size = Size(3.dp.toPx(), size.height)
                )
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
        ) {
            // Header row (tappable)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (expandableText != null) {
                            isExpanded = !isExpanded
                        }
                    }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Icon
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    AppColors.warmBorder,
                                    AppColors.warmBorder.copy(alpha = 0.8f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (feast.importance == "great") "\u2726" else "\u2626",
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = feast.name,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = AppColors.darkText
                    )
                    Text(
                        text = localizedType,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = AppColors.lightMuted
                    )
                }

                if (expandableText != null) {
                    Text(
                        text = if (isExpanded) "\u25B2" else "\u25BC",
                        fontSize = 10.sp,
                        color = AppColors.lightMuted,
                        modifier = Modifier.padding(top = 10.dp)
                    )
                }
            }

            // Expandable content
            if (isExpanded && expandableText != null) {
                Text(
                    text = expandableText,
                    fontFamily = FontFamily.Serif,
                    fontSize = 14.sp,
                    color = AppColors.bodyText,
                    lineHeight = 22.sp,
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp)
                )
            }
        }
    }
}

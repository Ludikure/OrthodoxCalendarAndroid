package com.orthodox.calendar.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object AppColors {
    // Brand colors (same in both modes)
    val gold = Color(0xFFD4AF37)
    val crimson = Color(0xFFC94040)
    val feastBlue = Color(0xFF588AB0)
    val holyWeekPurple = Color(0xFF8060A0)
    val brightGold = Color(0xFFD4C080)
    val fastFreeGreen = Color(0xFF5A8A50)

    // Fasting badge colors
    val fastStrict = Color(0xFF7B2D8E)
    val fastWater = Color(0xFF2E7D9B)
    val fastOil = Color(0xFF8B7B2D)
    val fastFish = Color(0xFF2D6B4F)
    val fastFree = Color(0xFF4A7C3F)

    // Adaptive colors - call these composable getters

    val headerBg: Color
        @Composable get() = if (LocalIsDarkTheme.current)
            Color(0xFF1A1410) else Color(0xFF2C2418)

    val warmBg: Color
        @Composable get() = if (LocalIsDarkTheme.current)
            Color(0xFF1C1A17) else Color(0xFFF5F3EE)

    val goldAccent: Color
        @Composable get() = if (LocalIsDarkTheme.current)
            Color(0xFFD1BD94) else Color(0xFFD4C5A9)

    val mutedText: Color
        @Composable get() = if (LocalIsDarkTheme.current)
            Color(0xFF998C7A) else Color(0xFF8C7E6A)

    val warmBorder: Color
        @Composable get() = if (LocalIsDarkTheme.current)
            Color(0xFF332E26) else Color(0xFFF0EDE8)

    val cardBg: Color
        @Composable get() = if (LocalIsDarkTheme.current)
            Color(0xFF241F1A) else Color(0xFFFAFAF7)

    val darkText: Color
        @Composable get() = if (LocalIsDarkTheme.current)
            Color(0xFFE6DED1) else Color(0xFF2C2418)

    val lightMuted: Color
        @Composable get() = if (LocalIsDarkTheme.current)
            Color(0xFF736659) else Color(0xFFB0A48E)

    val bodyText: Color
        @Composable get() = if (LocalIsDarkTheme.current)
            Color(0xFFBFB3A1) else Color(0xFF5C5040)

    // Fasting-season banner/badge — soft liturgical parchment with burgundy text.
    val bannerBg: Color
        @Composable get() = if (LocalIsDarkTheme.current)
            Color(0xFF322B1C) else Color(0xFFEFE2BC)

    val bannerTitle: Color
        @Composable get() = if (LocalIsDarkTheme.current)
            Color(0xFFDCC089) else Color(0xFF7A1F1A)

    val bannerSubtext: Color
        @Composable get() = if (LocalIsDarkTheme.current)
            Color(0xFF9A8A72) else Color(0xFF6B5B4A)

    // Fasting badge backgrounds
    val fastStrictBg: Color
        @Composable get() = if (LocalIsDarkTheme.current)
            Color(0xFF401F4D) else Color(0xFFF3E8F8)

    val fastWaterBg: Color
        @Composable get() = if (LocalIsDarkTheme.current)
            Color(0xFF1A3847) else Color(0xFFE4F2F8)

    val fastOilBg: Color
        @Composable get() = if (LocalIsDarkTheme.current)
            Color(0xFF40381A) else Color(0xFFFFF8E1)

    val fastFishBg: Color
        @Composable get() = if (LocalIsDarkTheme.current)
            Color(0xFF1A3326) else Color(0xFFE8F5EC)

    val fastFreeBg: Color
        @Composable get() = if (LocalIsDarkTheme.current)
            Color(0xFF24381F) else Color(0xFFEDF8EA)

    // Great feast row dark red text
    val greatFeastText = Color(0xFF8B1A1A)
}

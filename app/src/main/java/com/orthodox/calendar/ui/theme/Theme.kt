package com.orthodox.calendar.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import com.orthodox.calendar.data.model.AppTheme

/** Composition local that provides the resolved dark mode state to AppColors */
val LocalIsDarkTheme = compositionLocalOf { false }

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF2C2418),
    onPrimary = Color.White,
    secondary = Color(0xFFD4AF37),
    onSecondary = Color(0xFF2C2418),
    background = Color(0xFFF5F3EE),
    onBackground = Color(0xFF2C2418),
    surface = Color(0xFFFAFAF7),
    onSurface = Color(0xFF2C2418),
    surfaceVariant = Color(0xFFF0EDE8),
    onSurfaceVariant = Color(0xFF5C5040)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFD1BD94),
    onPrimary = Color(0xFF1A1410),
    secondary = Color(0xFFD4AF37),
    onSecondary = Color(0xFF1A1410),
    background = Color(0xFF1C1A17),
    onBackground = Color(0xFFE6DED1),
    surface = Color(0xFF241F1A),
    onSurface = Color(0xFFE6DED1),
    surfaceVariant = Color(0xFF332E26),
    onSurfaceVariant = Color(0xFFBFB3A1)
)

@Composable
fun OrthodoxCalendarTheme(
    appTheme: AppTheme = AppTheme.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (appTheme) {
        AppTheme.SYSTEM -> isSystemInDarkTheme()
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
    }

    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(LocalIsDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            content = content
        )
    }
}

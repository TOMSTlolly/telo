package com.tomst.lolly.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = BrandGreen,
    onPrimary = Color.White,
    primaryContainer = BrandGreenLight,
    onPrimaryContainer = BrandGreenDark,
    secondary = BrandGreen,
    onSecondary = Color.White,
    tertiary = BrandGreenDark,
    onTertiary = Color.White,
    background = Color(0xFFF4F9F5), // Slight green hue for backgrounds
    onBackground = Color(0xFF1A1C1A),
    surface = Color.White,
    onSurface = Color(0xFF1A1C1A),
    surfaceVariant = Color(0xFFE2EBE4),
    onSurfaceVariant = Color(0xFF414942),
    outline = Color(0xFF717972)
)

@Composable
fun LollyTheme(
    darkTheme: Boolean = false, // CRITICAL: Forced false to avoid accidental dark mode inversions
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false, // CRITICAL: Set to false to enforce Brand Green
    content: @Composable () -> Unit
) {
    val colorScheme = LightColorScheme // Force light scheme to keep backgrounds white/light green

    val view = LocalView.current
    if (!view.isInEditMode) {
        @Suppress("DEPRECATION")
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        // typography = Typography, // Uncomment if you are using a custom Typography.kt
        content = content
    )
}

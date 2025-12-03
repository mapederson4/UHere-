package com.cs407.uhere.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Professional Blue Color Palette
private val PrimaryBlue = Color(0xFF1565C0)  // Deep professional blue
private val PrimaryBlueLight = Color(0xFF1976D2)  // Lighter blue
private val PrimaryBlueDark = Color(0xFF0D47A1)  // Darker blue
private val AccentTeal = Color(0xFF00ACC1)  // Complementary teal
private val AccentOrange = Color(0xFFFF6F00)  // Warm accent for CTAs
private val SuccessGreen = Color(0xFF2E7D32)  // For progress/success
private val BackgroundLight = Color(0xFFF5F7FA)  // Soft background
private val SurfaceLight = Color(0xFFFFFFFF)  // White surface
private val TextPrimary = Color(0xFF1A1A1A)  // Almost black
private val TextSecondary = Color(0xFF757575)  // Gray

// Dark theme colors
private val DarkBackground = Color(0xFF0A1929)  // Navy dark
private val DarkSurface = Color(0xFF132F4C)  // Lighter navy
private val DarkPrimary = Color(0xFF5FA8D3)  // Lighter blue for dark mode

private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFBBDEFB),
    onPrimaryContainer = PrimaryBlueDark,

    secondary = AccentTeal,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFB2EBF2),
    onSecondaryContainer = Color(0xFF006064),

    tertiary = AccentOrange,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE0B2),
    onTertiaryContainer = Color(0xFFE65100),

    background = BackgroundLight,
    onBackground = TextPrimary,

    surface = SurfaceLight,
    onSurface = TextPrimary,
    surfaceVariant = Color(0xFFE3F2FD),
    onSurfaceVariant = TextSecondary,

    error = Color(0xFFD32F2F),
    onError = Color.White,
    errorContainer = Color(0xFFFFCDD2),
    onErrorContainer = Color(0xFFB71C1C),

    outline = Color(0xFFBDBDBD),
    outlineVariant = Color(0xFFE0E0E0)
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = Color.White,
    primaryContainer = PrimaryBlueDark,
    onPrimaryContainer = Color(0xFFBBDEFB),

    secondary = AccentTeal,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF00838F),
    onSecondaryContainer = Color(0xFFB2EBF2),

    tertiary = AccentOrange,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE65100),
    onTertiaryContainer = Color(0xFFFFE0B2),

    background = DarkBackground,
    onBackground = Color.White,

    surface = DarkSurface,
    onSurface = Color.White,
    surfaceVariant = Color(0xFF1E3A5F),
    onSurfaceVariant = Color(0xFFB0BEC5),

    error = Color(0xFFEF5350),
    onError = Color.White,
    errorContainer = Color(0xFFC62828),
    onErrorContainer = Color(0xFFFFCDD2),

    outline = Color(0xFF546E7A),
    outlineVariant = Color(0xFF37474F)
)

@Composable
fun UHereTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        DarkColorScheme
    } else {
        LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
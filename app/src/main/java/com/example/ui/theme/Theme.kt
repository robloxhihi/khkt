package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// 1. Default Blue / Neon Violet Theme
private val VesperaDarkColorScheme = darkColorScheme(
    primary = NeonViolet,
    onPrimary = TextOnAccent,
    primaryContainer = DarkCardElevated,
    onPrimaryContainer = DarkTextPrimary,
    secondary = NeonCyan,
    onSecondary = Color(0xFF00363F),
    secondaryContainer = DarkCardSurface,
    onSecondaryContainer = DarkTextSecondary,
    tertiary = NeonAmber,
    onTertiary = Color(0xFF452B00),
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkCardSurface,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkCardBorder,
    error = NeonRose,
    onError = TextOnAccent
)

private val VesperaLightColorScheme = lightColorScheme(
    primary = NeonViolet,
    onPrimary = TextOnAccent,
    primaryContainer = LightCardElevated,
    onPrimaryContainer = LightTextPrimary,
    secondary = Color(0xFF0284C7),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE0F2FE),
    onSecondaryContainer = Color(0xFF0369A1),
    tertiary = Color(0xFFD97706),
    onTertiary = Color(0xFFFFFFFF),
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightCardSurface,
    onSurfaceVariant = LightTextSecondary,
    outline = LightCardBorder,
    error = NeonRose,
    onError = TextOnAccent
)

// 2. Emerald Green Theme
private val EmeraldDarkColorScheme = darkColorScheme(
    primary = EmeraldPrimary,
    onPrimary = TextOnAccent,
    primaryContainer = DarkCardElevated,
    onPrimaryContainer = DarkTextPrimary,
    secondary = NeonEmerald,
    onSecondary = Color(0xFF003816),
    secondaryContainer = DarkCardSurface,
    onSecondaryContainer = DarkTextSecondary,
    tertiary = NeonAmber,
    onTertiary = Color(0xFF452B00),
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkCardSurface,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkCardBorder,
    error = NeonRose,
    onError = TextOnAccent
)

private val EmeraldLightColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = TextOnAccent,
    primaryContainer = Color(0xFFD1FAE5),
    onPrimaryContainer = Color(0xFF065F46),
    secondary = NeonEmerald,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFECFDF5),
    onSecondaryContainer = Color(0xFF047857),
    tertiary = NeonAmber,
    onTertiary = Color(0xFF452B00),
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightCardSurface,
    onSurfaceVariant = LightTextSecondary,
    outline = LightCardBorder,
    error = NeonRose,
    onError = TextOnAccent
)

// 3. Sunset Rose Theme
private val SunsetDarkColorScheme = darkColorScheme(
    primary = SunsetPrimary,
    onPrimary = TextOnAccent,
    primaryContainer = DarkCardElevated,
    onPrimaryContainer = DarkTextPrimary,
    secondary = NeonAmber,
    onSecondary = Color(0xFF452B00),
    secondaryContainer = DarkCardSurface,
    onSecondaryContainer = DarkTextSecondary,
    tertiary = NeonCyan,
    onTertiary = Color(0xFF00363F),
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkCardSurface,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkCardBorder,
    error = NeonRose,
    onError = TextOnAccent
)

private val SunsetLightColorScheme = lightColorScheme(
    primary = SunsetPrimary,
    onPrimary = TextOnAccent,
    primaryContainer = Color(0xFFFFE4E6),
    onPrimaryContainer = Color(0xFF9F1239),
    secondary = NeonAmber,
    onSecondary = Color(0xFF452B00),
    secondaryContainer = Color(0xFFFEF3C7),
    onSecondaryContainer = Color(0xFFB45309),
    tertiary = NeonCyan,
    onTertiary = Color(0xFF00363F),
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightCardSurface,
    onSurfaceVariant = LightTextSecondary,
    outline = LightCardBorder,
    error = NeonRose,
    onError = TextOnAccent
)

// 4. Royal Purple Theme
private val PurpleDarkColorScheme = darkColorScheme(
    primary = PurplePrimary,
    onPrimary = TextOnAccent,
    primaryContainer = DarkCardElevated,
    onPrimaryContainer = DarkTextPrimary,
    secondary = NeonCyan,
    onSecondary = Color(0xFF00363F),
    secondaryContainer = DarkCardSurface,
    onSecondaryContainer = DarkTextSecondary,
    tertiary = NeonAmber,
    onTertiary = Color(0xFF452B00),
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkCardSurface,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkCardBorder,
    error = NeonRose,
    onError = TextOnAccent
)

private val PurpleLightColorScheme = lightColorScheme(
    primary = PurplePrimary,
    onPrimary = TextOnAccent,
    primaryContainer = Color(0xFFEDE9FE),
    onPrimaryContainer = Color(0xFF5B21B6),
    secondary = NeonCyan,
    onSecondary = Color(0xFF00363F),
    secondaryContainer = Color(0xFFE0F2FE),
    onSecondaryContainer = Color(0xFF0369A1),
    tertiary = NeonAmber,
    onTertiary = Color(0xFF452B00),
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightCardSurface,
    onSurfaceVariant = LightTextSecondary,
    outline = LightCardBorder,
    error = NeonRose,
    onError = TextOnAccent
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    colorTheme: String = "blue",
    content: @Composable () -> Unit
) {
    val colorScheme = when (colorTheme) {
        "emerald" -> if (darkTheme) EmeraldDarkColorScheme else EmeraldLightColorScheme
        "sunset" -> if (darkTheme) SunsetDarkColorScheme else SunsetLightColorScheme
        "purple" -> if (darkTheme) PurpleDarkColorScheme else PurpleLightColorScheme
        else -> if (darkTheme) VesperaDarkColorScheme else VesperaLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = VesperaTypography,
        content = content
    )
}

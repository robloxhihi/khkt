package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val VesperaDarkColorScheme = darkColorScheme(
    primary = NeonViolet,
    onPrimary = TextOnAccent,
    primaryContainer = SpaceCardElevated,
    onPrimaryContainer = TextPrimary,
    secondary = NeonCyan,
    onSecondary = Color(0xFF00363F),
    secondaryContainer = SpaceCardSurface,
    onSecondaryContainer = TextSecondary,
    tertiary = NeonAmber,
    onTertiary = Color(0xFF452B00),
    background = SpaceDarkBackground,
    onBackground = TextPrimary,
    surface = SpaceDarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = SpaceCardSurface,
    onSurfaceVariant = TextSecondary,
    outline = SpaceCardBorder,
    error = NeonRose,
    onError = TextOnAccent
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Vespera Family is deeply committed to the signature Deep Space Dark Bento aesthetic
    MaterialTheme(
        colorScheme = VesperaDarkColorScheme,
        typography = VesperaTypography,
        content = content
    )
}

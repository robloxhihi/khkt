package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

// Light Theme Base Colors
val LightBackground = Color(0xFFF2F4F7)
val LightSurface = Color(0xFFFFFFFF)
val LightCardSurface = Color(0xFFFFFFFF)
val LightCardElevated = Color(0xFFE4E7EB)
val LightCardBorder = Color(0xFFD0D5DD)
val LightTextPrimary = Color(0xFF0F172A) // Crisp dark black
val LightTextSecondary = Color(0xFF334155) // Dark slate
val LightTextMuted = Color(0xFF64748B) // Medium slate

// Dark Theme Base Colors
val DarkBackground = Color(0xFF18191A)
val DarkSurface = Color(0xFF242526)
val DarkCardSurface = Color(0xFF242526)
val DarkCardElevated = Color(0xFF3A3B3C)
val DarkCardBorder = Color(0xFF3E4042)
val DarkTextPrimary = Color(0xFFE4E6EB)
val DarkTextSecondary = Color(0xFFB0B3B8)
val DarkTextMuted = Color(0xFF8A8D91)
val TextOnAccent = Color(0xFFFFFFFF)

// Palette Accents
val NeonViolet = Color(0xFF2D88FF)
val NeonCyan = Color(0xFF00C6FF)
val NeonEmerald = Color(0xFF31A24C)
val NeonAmber = Color(0xFFF7B125)
val NeonRose = Color(0xFFFA383E)

// Alternate Palettes
val EmeraldPrimary = Color(0xFF10B981)
val SunsetPrimary = Color(0xFFF43F5E)
val PurplePrimary = Color(0xFF8B5CF6)

// Emotion & Status Accents
val EmotionHappy = Color(0xFF31A24C)
val EmotionNormal = Color(0xFF2D88FF)
val EmotionStudyStress = Color(0xFFF7B125)
val EmotionNeedSupport = Color(0xFFFA383E)

// Dynamic Color Accessors: In Light Mode, text becomes pure black/slate; in Dark Mode, light grey
val TextPrimary: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.onSurface

val TextSecondary: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.onSurfaceVariant

val TextMuted: Color
    @Composable
    @ReadOnlyComposable
    get() = if (MaterialTheme.colorScheme.surface == LightSurface) LightTextMuted else DarkTextMuted

val SpaceCardSurface: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.surfaceVariant

val SpaceCardElevated: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.primaryContainer

val SpaceCardBorder: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.outline

val SpaceDarkBackground: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.background

val SpaceDarkSurface: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.surface

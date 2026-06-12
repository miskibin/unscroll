package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Mint = Color(0xFF7BE0AD)
private val MintDim = Color(0xFF3E8E67)
private val Coral = Color(0xFFFF7B6B)
private val Ink = Color(0xFF101418)
private val InkSurface = Color(0xFF1A2026)
private val InkSurfaceHigh = Color(0xFF232B33)
private val Mist = Color(0xFFE8EDF2)
private val MistDim = Color(0xFF9AA7B4)

private val DarkColors = darkColorScheme(
    primary = Mint,
    onPrimary = Ink,
    secondary = MistDim,
    error = Coral,
    background = Ink,
    onBackground = Mist,
    surface = InkSurface,
    onSurface = Mist,
    surfaceVariant = InkSurfaceHigh,
    onSurfaceVariant = MistDim,
    outline = Color(0xFF35404B),
)

private val LightColors = lightColorScheme(
    primary = MintDim,
    onPrimary = Color.White,
    error = Coral,
    background = Color(0xFFF4F7FA),
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Color(0xFFE4EAF0),
    onSurfaceVariant = Color(0xFF5B6873),
    outline = Color(0xFFC4CDD6),
)

@Composable
fun UnscrollTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = UnscrollTypography,
        content = content,
    )
}

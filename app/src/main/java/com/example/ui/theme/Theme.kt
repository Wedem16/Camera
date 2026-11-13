package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val GalaxyCameraDarkColorScheme = darkColorScheme(
    primary = GalaxyAmber,
    onPrimary = AmoledBlack,
    primaryContainer = ObsidianElevated,
    onPrimaryContainer = GalaxyAmber,
    secondary = AiCyan,
    onSecondary = AmoledBlack,
    secondaryContainer = ObsidianElevated,
    onSecondaryContainer = AiCyan,
    tertiary = RecordCrimson,
    onTertiary = TextPrimaryWhite,
    background = AmoledBlack,
    onBackground = TextPrimaryWhite,
    surface = ObsidianSurface,
    onSurface = TextPrimaryWhite,
    surfaceVariant = ObsidianElevated,
    onSurfaceVariant = TextSecondaryZinc,
    error = RecordCrimson,
    onError = TextPrimaryWhite
)

@Composable
fun GalaxyCameraTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = GalaxyCameraDarkColorScheme,
        typography = Typography,
        content = content
    )
}

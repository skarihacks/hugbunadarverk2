package com.hbv501g.forumapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = NordicInk,
    onPrimary = NordicSnow,
    primaryContainer = NordicFrost,
    onPrimaryContainer = NordicInk,
    secondary = NordicStone,
    onSecondary = NordicSnow,
    secondaryContainer = NordicMist,
    onSecondaryContainer = NordicInk,
    tertiary = NordicPine,
    background = NordicSnow,
    onBackground = NordicInk,
    surface = NordicSurface,
    onSurface = NordicInk,
    surfaceVariant = NordicFrost,
    onSurfaceVariant = NordicStone,
    outline = NordicBorder,
    error = NordicError,
    onError = NordicSnow
)

private val DarkColors = darkColorScheme(
    primary = NordicFrost,
    onPrimary = NordicNight,
    primaryContainer = NordicNightSurface,
    onPrimaryContainer = NordicSnow,
    secondary = NordicNightMuted,
    onSecondary = NordicNight,
    tertiary = NordicMist,
    background = NordicNight,
    onBackground = NordicSnow,
    surface = NordicNightSurface,
    onSurface = NordicSnow,
    surfaceVariant = Color(0xFF203A45),
    onSurfaceVariant = NordicNightMuted,
    outline = Color(0xFF35515B),
    error = NordicError,
    onError = NordicSnow
)

@Composable
fun ForumTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}

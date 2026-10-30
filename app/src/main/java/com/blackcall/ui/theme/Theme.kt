package com.blackcall.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val BlackCallColors = darkColorScheme(
    primary = Teal,
    onPrimary = Black,
    primaryContainer = TealDeep,
    onPrimaryContainer = White,
    secondary = Green,
    onSecondary = Black,
    secondaryContainer = Color(0xFF14532D),
    onSecondaryContainer = White,
    tertiary = TealBright,
    onTertiary = Black,
    background = Black,
    onBackground = White,
    surface = Surface0,
    onSurface = White,
    surfaceVariant = Surface2,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLowest = Black,
    surfaceContainerLow = Surface0,
    surfaceContainer = Surface1,
    surfaceContainerHigh = Surface2,
    surfaceContainerHighest = Surface2,
    outline = TextTertiary,
    outlineVariant = Hairline,
    error = Coral,
    onError = Black,
    scrim = Black
)

/** Black Call is dark-only: the gradient identity depends on a black canvas. */
@Composable
fun BlackCallTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = BlackCallColors,
        typography = BlackCallTypography,
        content = content
    )
}

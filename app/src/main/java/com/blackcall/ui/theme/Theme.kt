package com.blackcall.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.blackcall.domain.Appearance
import com.blackcall.domain.ThemeMode

fun resolveDark(appearance: Appearance, systemDark: Boolean): Boolean = when (appearance.mode) {
    ThemeMode.SYSTEM -> systemDark
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

private fun BcColors.toMaterial(): ColorScheme =
    if (isDark) darkColorScheme(
        primary = start, onPrimary = Black,
        primaryContainer = deep, onPrimaryContainer = White,
        secondary = end, onSecondary = Black,
        secondaryContainer = Color(0xFF14532D), onSecondaryContainer = White,
        tertiary = accent, onTertiary = Black,
        background = background, onBackground = White,
        surface = Color(0xFF081213), onSurface = White,
        surfaceVariant = surfaceHigh, onSurfaceVariant = textSecondary,
        surfaceContainerLowest = background, surfaceContainerLow = Color(0xFF081213),
        surfaceContainer = surface, surfaceContainerHigh = surfaceHigh, surfaceContainerHighest = surfaceHigh,
        outline = textTertiary, outlineVariant = hairline,
        error = Coral, onError = Black, scrim = Black
    ) else lightColorScheme(
        primary = accent, onPrimary = White,
        primaryContainer = surfaceHigh, onPrimaryContainer = text,
        secondary = accent2, onSecondary = White,
        tertiary = accent, onTertiary = White,
        background = background, onBackground = text,
        surface = surface, onSurface = text,
        surfaceVariant = surfaceHigh, onSurfaceVariant = textSecondary,
        surfaceContainerLowest = background, surfaceContainerLow = surface,
        surfaceContainer = surface, surfaceContainerHigh = surfaceHigh, surfaceContainerHighest = surfaceHigh,
        outline = textTertiary, outlineVariant = hairline,
        error = Coral, onError = Black, scrim = Black
    )

/**
 * Applies the user's appearance. With the defaults (dark, pure black, emerald, motion on)
 * this is the original Black Call look.
 */
@Composable
fun BlackCallTheme(
    appearance: Appearance,
    systemDark: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val dark = resolveDark(appearance, systemDark)
    val colors = remember(dark, appearance.amoled, appearance.accent) {
        buildColors(dark, appearance.amoled, paletteFor(appearance.accent))
    }
    val reduced = appearance.reduceMotion || rememberSystemReducedMotion()
    CompositionLocalProvider(LocalBc provides colors, LocalReducedMotion provides reduced) {
        MaterialTheme(colorScheme = colors.toMaterial(), typography = BlackCallTypography, content = content)
    }
}

package com.blackcall.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Fixed colours: the same in every theme.
val Black = Color(0xFF000000)
val Ink = Color(0xFF050B0C)
val White = Color(0xFFFFFFFF)

/** Only used for destructive actions, where green/teal would read as "safe". */
val Coral = Color(0xFFFF6B6B)

/** Original (emerald) brand pair, for places that can't read the theme. */
val BrandColors = listOf(Color(0xFF14B8A6), Color(0xFF22C55E))

// Theme-aware colours. Same names as always, but they follow the user's theme mode and accent
// (see Palette.kt). Read them inside composable code; for draw-scope lambdas, copy into a local first.

val Surface1: Color @Composable @ReadOnlyComposable get() = LocalBc.current.surface
val Surface2: Color @Composable @ReadOnlyComposable get() = LocalBc.current.surfaceHigh

val Teal: Color @Composable @ReadOnlyComposable get() = LocalBc.current.start
val Green: Color @Composable @ReadOnlyComposable get() = LocalBc.current.end
val TealBright: Color @Composable @ReadOnlyComposable get() = LocalBc.current.accent
val GreenBright: Color @Composable @ReadOnlyComposable get() = LocalBc.current.accent2
val TealDeep: Color @Composable @ReadOnlyComposable get() = LocalBc.current.deep

val TextPrimary: Color @Composable @ReadOnlyComposable get() = LocalBc.current.text
val TextSecondary: Color @Composable @ReadOnlyComposable get() = LocalBc.current.textSecondary
val TextTertiary: Color @Composable @ReadOnlyComposable get() = LocalBc.current.textTertiary
val Hairline: Color @Composable @ReadOnlyComposable get() = LocalBc.current.hairline

val GlassTop: Color @Composable @ReadOnlyComposable get() = LocalBc.current.glassTop
val GlassBottom: Color @Composable @ReadOnlyComposable get() = LocalBc.current.glassBottom
val GlassBorderTop: Color @Composable @ReadOnlyComposable get() = LocalBc.current.glassBorderTop
val GlassBorderBottom: Color @Composable @ReadOnlyComposable get() = LocalBc.current.glassBorderBottom

val SubtleLow: Color @Composable @ReadOnlyComposable get() = LocalBc.current.subtleLow
val Subtle: Color @Composable @ReadOnlyComposable get() = LocalBc.current.subtle
val SubtleMid: Color @Composable @ReadOnlyComposable get() = LocalBc.current.subtleMid
val SubtleStrong: Color @Composable @ReadOnlyComposable get() = LocalBc.current.subtleStrong

val BarBackground: Color @Composable @ReadOnlyComposable get() = LocalBc.current.barBackground
val BarBorderTop: Color @Composable @ReadOnlyComposable get() = LocalBc.current.barBorderTop
val BarBorderBottom: Color @Composable @ReadOnlyComposable get() = LocalBc.current.barBorderBottom

val BrandGradient: Brush @Composable @ReadOnlyComposable get() = LocalBc.current.brand
val BrandGradientBright: Brush @Composable @ReadOnlyComposable get() = LocalBc.current.brandText

/** Page background: accent-lit at the top, falling to the base colour. */
val ScreenGradient: Brush @Composable @ReadOnlyComposable get() = LocalBc.current.screenGradient

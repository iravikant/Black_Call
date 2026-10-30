package com.blackcall.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Palette: Black, Teal, Green, White.
val Black = Color(0xFF000000)
val Ink = Color(0xFF050B0C)
val Surface0 = Color(0xFF081213)
val Surface1 = Color(0xFF0D1B1C)
val Surface2 = Color(0xFF132627)

val Teal = Color(0xFF14B8A6)
val TealBright = Color(0xFF2DD4BF)
val TealDeep = Color(0xFF0F766E)
val Green = Color(0xFF22C55E)
val GreenBright = Color(0xFF4ADE80)
val White = Color(0xFFFFFFFF)

val TextPrimary = White
val TextSecondary = Color(0xB3FFFFFF) // 70% white
val TextTertiary = Color(0x80FFFFFF)  // 50% white
val Hairline = Color(0x1FFFFFFF)      // 12% white

/** Only used for destructive actions, where green/teal would read as "safe". */
val Coral = Color(0xFFFF6B6B)

val BrandGradient: Brush = Brush.linearGradient(listOf(Teal, Green))
val BrandGradientBright: Brush = Brush.linearGradient(listOf(TealBright, GreenBright))
val BrandColors = listOf(Teal, Green)

/** Page background: teal-lit at the top, falling to pure black. */
val ScreenGradient: Brush = Brush.verticalGradient(
    colorStops = arrayOf(
        0.0f to Color(0xFF06302B),
        0.35f to Color(0xFF031614),
        1.0f to Black
    )
)

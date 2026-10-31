package com.blackcall.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.blackcall.domain.AccentId

/** Brand colours for one accent. "bright" reads on black; "deep" reads on white. */
@Immutable
data class AccentPalette(
    val start: Color,
    val end: Color,
    val brightStart: Color,
    val brightEnd: Color,
    val deepStart: Color,
    val deepEnd: Color
)

fun paletteFor(id: AccentId): AccentPalette = when (id) {
    // Emerald is the original Black Call palette.
    AccentId.EMERALD -> AccentPalette(
        Color(0xFF14B8A6), Color(0xFF22C55E), Color(0xFF2DD4BF), Color(0xFF4ADE80), Color(0xFF0F766E), Color(0xFF15803D)
    )
    AccentId.AQUA -> AccentPalette(
        Color(0xFF06B6D4), Color(0xFF14B8A6), Color(0xFF22D3EE), Color(0xFF2DD4BF), Color(0xFF0E7490), Color(0xFF0F766E)
    )
    AccentId.FOREST -> AccentPalette(
        Color(0xFF16A34A), Color(0xFF22C55E), Color(0xFF4ADE80), Color(0xFF86EFAC), Color(0xFF166534), Color(0xFF15803D)
    )
    AccentId.MINT -> AccentPalette(
        Color(0xFF2DD4BF), Color(0xFF86EFAC), Color(0xFF99F6E4), Color(0xFFBBF7D0), Color(0xFF0F766E), Color(0xFF166534)
    )
}

/**
 * Every colour the UI uses, resolved for the current light/dark mode and accent.
 * With the default settings (dark, pure black, emerald) these equal the app's original colours.
 */
@Immutable
data class BcColors(
    val isDark: Boolean,
    val screenGradient: Brush,
    val background: Color,
    val surface: Color,
    val surfaceHigh: Color,
    val text: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val hairline: Color,
    val glassTop: Color,
    val glassBottom: Color,
    val glassBorderTop: Color,
    val glassBorderBottom: Color,
    val subtleLow: Color,
    val subtle: Color,
    val subtleMid: Color,
    val subtleStrong: Color,
    val barBackground: Color,
    val barBorderTop: Color,
    val barBorderBottom: Color,
    /** Accent for text and icons: bright on dark, deep on light. */
    val accent: Color,
    val accent2: Color,
    val deep: Color,
    /** Fill colours for gradients and buttons. */
    val start: Color,
    val end: Color,
    val brand: Brush,
    val brandText: Brush
)

fun buildColors(dark: Boolean, amoled: Boolean, p: AccentPalette): BcColors {
    val brand = Brush.linearGradient(listOf(p.start, p.end))
    return if (dark) {
        val bottom = if (amoled) Color.Black else Color(0xFF071012)
        BcColors(
            isDark = true,
            screenGradient = Brush.verticalGradient(
                0f to p.start.copy(alpha = 0.26f).compositeOver(bottom),
                0.35f to p.start.copy(alpha = 0.12f).compositeOver(bottom),
                1f to bottom
            ),
            background = bottom,
            surface = Color(0xFF0D1B1C),
            surfaceHigh = Color(0xFF132627),
            text = Color.White,
            textSecondary = Color(0xB3FFFFFF),
            textTertiary = Color(0x80FFFFFF),
            hairline = Color(0x1FFFFFFF),
            glassTop = Color(0x1AFFFFFF),
            glassBottom = Color(0x08FFFFFF),
            glassBorderTop = Color(0x33FFFFFF),
            glassBorderBottom = Color(0x0DFFFFFF),
            subtleLow = Color(0x12FFFFFF),
            subtle = Color(0x14FFFFFF),
            subtleMid = Color(0x1AFFFFFF),
            subtleStrong = Color(0x26FFFFFF),
            barBackground = Color(0xE60A1415),
            barBorderTop = Color(0x40FFFFFF),
            barBorderBottom = Color(0x10FFFFFF),
            accent = p.brightStart,
            accent2 = p.brightEnd,
            deep = p.deepStart,
            start = p.start,
            end = p.end,
            brand = brand,
            brandText = Brush.linearGradient(listOf(p.brightStart, p.brightEnd))
        )
    } else {
        val ink = Color(0xFF07211F)
        BcColors(
            isDark = false,
            screenGradient = Brush.verticalGradient(
                0f to p.start.copy(alpha = 0.22f).compositeOver(Color.White),
                0.4f to p.start.copy(alpha = 0.07f).compositeOver(Color.White),
                1f to Color.White
            ),
            background = Color.White,
            surface = Color.White,
            surfaceHigh = Color(0xFFE9F5F2),
            text = ink,
            textSecondary = ink.copy(alpha = 0.68f),
            textTertiary = ink.copy(alpha = 0.48f),
            hairline = ink.copy(alpha = 0.12f),
            glassTop = Color(0xE6FFFFFF),
            glassBottom = Color(0xB3FFFFFF),
            glassBorderTop = ink.copy(alpha = 0.10f),
            glassBorderBottom = ink.copy(alpha = 0.05f),
            subtleLow = ink.copy(alpha = 0.04f),
            subtle = ink.copy(alpha = 0.06f),
            subtleMid = ink.copy(alpha = 0.08f),
            subtleStrong = ink.copy(alpha = 0.12f),
            barBackground = Color(0xF5FFFFFF),
            barBorderTop = ink.copy(alpha = 0.10f),
            barBorderBottom = ink.copy(alpha = 0.05f),
            accent = p.deepStart,
            accent2 = p.deepEnd,
            deep = p.deepStart,
            start = p.start,
            end = p.end,
            brand = brand,
            brandText = Brush.linearGradient(listOf(p.deepStart, p.deepEnd))
        )
    }
}

private fun Color.compositeOver(background: Color): Color {
    val a = alpha
    return Color(
        red = red * a + background.red * (1 - a),
        green = green * a + background.green * (1 - a),
        blue = blue * a + background.blue * (1 - a),
        alpha = 1f
    )
}

val LocalBc = staticCompositionLocalOf<BcColors> { error("BlackCallTheme not provided") }

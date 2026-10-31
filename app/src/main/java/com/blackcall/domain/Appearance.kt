package com.blackcall.domain

enum class ThemeMode(val label: String) { SYSTEM("System"), LIGHT("Light"), DARK("Dark") }

enum class AccentId(val label: String) {
    EMERALD("Emerald"),
    AQUA("Aqua"),
    FOREST("Forest"),
    MINT("Mint")
}

data class Appearance(
    /** Defaults to dark so the app looks exactly as it always has until the user changes it. */
    val mode: ThemeMode = ThemeMode.DARK,
    /** Pure-black backgrounds in dark mode (saves power on OLED). */
    val amoled: Boolean = true,
    val accent: AccentId = AccentId.EMERALD,
    /** Turns off decorative motion. The system "remove animations" setting also forces this on. */
    val reduceMotion: Boolean = false
)

data class Profile(val name: String = "") {
    /** First letter of the first and last word (max two letters); empty when no name is set. */
    fun initials(): String {
        val words = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        val picked = if (words.size >= 2) listOf(words.first(), words.last()) else words
        return picked.mapNotNull { w -> w.firstOrNull { it.isLetterOrDigit() } }.joinToString("").uppercase()
    }

    companion object { const val MAX_NAME = 30 }
}

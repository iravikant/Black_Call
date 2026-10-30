package com.blackcall.domain

data class SpamAssessment(val score: Int, val signals: List<String>)

/**
 * Transparent, rule-based spam scoring. Every point in the score comes from a
 * named signal so the call log can show the user *why* a call was blocked.
 * Tuned for Indian numbering (140 telemarketing series); adjust for other regions.
 */
object SpamScorer {

    // Word boundaries matter: the old "vi" keyword matched names like "Vivek".
    private val labelPattern = Regex(
        "\\b(spam|scam|fraud|telemarket(?:er|ing)|robo-?call|nuisance|junk|harass(?:ment)?)\\b",
        RegexOption.IGNORE_CASE
    )

    fun assess(rawNumber: String, callerLabel: String?): SpamAssessment {
        val key = PhoneNumbers.key(rawNumber)
        val signals = mutableListOf<String>()
        var score = 0

        if (!callerLabel.isNullOrBlank() && labelPattern.containsMatchIn(callerLabel)) {
            score += 85
            signals += "Flagged as spam by your dialer or carrier"
        }
        if (key.startsWith("140")) {
            score += 65
            signals += "Telemarketing series (140)"
        } else if (key.startsWith("1800") || key.startsWith("1860")) {
            score += 20
            signals += "Commercial toll-free number"
        }
        if (key.length >= 7 && key.toSet().size <= 2) {
            score += 45
            signals += "Repeated digits"
        }
        if (key.length >= 7 && isSequential(key)) {
            score += 35
            signals += "Sequential digits"
        }
        if (key.length in 1..6) {
            score += 35
            signals += "Unusually short number"
        }
        return SpamAssessment(score.coerceAtMost(100), signals)
    }

    private fun isSequential(n: String): Boolean {
        var steps = 0
        for (i in 0 until n.length - 1) {
            val a = n[i].digitToIntOrNull() ?: continue
            val b = n[i + 1].digitToIntOrNull() ?: continue
            if (b == a + 1 || b == a - 1) steps++
        }
        return steps > n.length * 0.7
    }
}

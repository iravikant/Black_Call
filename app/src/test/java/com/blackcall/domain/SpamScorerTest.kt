package com.blackcall.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpamScorerTest {

    @Test fun ordinaryNumberScoresZero() {
        assertEquals(0, SpamScorer.assess("9415820736", null).score)
    }

    @Test fun telemarketingSeriesIsFlagged() {
        val a = SpamScorer.assess("1409876543", null)
        assertTrue(a.score >= 55)
        assertTrue(a.signals.any { it.contains("140") })
    }

    @Test fun carrierSpamLabelIsFlagged() {
        assertTrue(SpamScorer.assess("9415820736", "Suspected Spam").score >= 75)
    }

    @Test fun namesContainingOperatorLettersAreNotSpam() {
        // Regression: the original code matched "vi" inside names like "Vivek".
        assertEquals(0, SpamScorer.assess("9415820736", "Vivek Sharma").score)
        assertEquals(0, SpamScorer.assess("9415820736", "Jio Store").score)
    }

    @Test fun repeatedAndSequentialDigits() {
        assertTrue(SpamScorer.assess("9999999999", null).signals.contains("Repeated digits"))
        assertTrue(SpamScorer.assess("1234567890", null).signals.contains("Sequential digits"))
    }

    @Test fun tollFreeAloneStaysBelowDefaultThreshold() {
        // Banks and delivery apps use 1800 numbers, so it must not block by itself.
        val a = SpamScorer.assess("18001234567", null)
        assertTrue(a.score < Sensitivity.BALANCED.threshold)
    }

    @Test fun scoreIsCapped() {
        assertTrue(SpamScorer.assess("1400000000", "spam scam").score <= 100)
    }
}

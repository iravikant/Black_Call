package com.blackcall.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BlockEngineTest {

    private val contacts = setOf("9811111111")
    private val engine = BlockEngine { PhoneNumbers.key(it) in contacts }

    private fun call(
        number: String?,
        label: String? = null,
        spoofed: Boolean = false,
        emergency: Boolean = false,
        minute: Int = 12 * 60
    ) = IncomingCall(number, label, emergency, spoofed, minute)

    private fun snap(
        settings: FilterSettings = FilterSettings(),
        blocked: Set<String> = emptySet(),
        allowed: Set<String> = emptySet(),
        prefixes: List<String> = emptyList()
    ) = RuleSnapshot(settings, blocked, allowed, prefixes)

    private fun Decision.reason() = (this as Decision.Block).reason

    @Test fun emergencyIsAlwaysAllowedEvenInStrictModes() {
        val s = snap(FilterSettings(contactsOnly = true, blockUnknown = true), blocked = setOf("112"))
        assertTrue(engine.decide(call("112", emergency = true), s) is Decision.Allow)
    }

    @Test fun disabledProtectionAllowsEverything() {
        val s = snap(FilterSettings(protectionEnabled = false), blocked = setOf("9876543210"))
        assertTrue(engine.decide(call("9876543210"), s) is Decision.Allow)
    }

    @Test fun blockedListWinsOverContacts() {
        val s = snap(blocked = setOf("9811111111"))
        assertEquals(BlockReason.MANUAL, engine.decide(call("9811111111"), s).reason())
    }

    @Test fun allowedListBeatsEverythingElse() {
        val s = snap(
            FilterSettings(contactsOnly = true),
            allowed = setOf("1409876543"),
            prefixes = listOf("140")
        )
        assertTrue(engine.decide(call("1409876543"), s) is Decision.Allow)
    }

    @Test fun prefixRuleBlocks() {
        val s = snap(prefixes = listOf("+92"))
        assertEquals(BlockReason.PREFIX, engine.decide(call("+923001234567"), s).reason())
    }

    @Test fun contactsAreNeverBlockedByUnknownOrSpamFilters() {
        val s = snap(FilterSettings(blockUnknown = true, contactsOnly = true))
        assertTrue(engine.decide(call("+91 98111 11111"), s) is Decision.Allow)
    }

    @Test fun contactsOnlyBlocksStrangers() {
        val s = snap(FilterSettings(contactsOnly = true))
        assertEquals(BlockReason.NOT_ALLOWED, engine.decide(call("9000000001"), s).reason())
    }

    @Test fun unknownFilterBlocksStrangers() {
        val s = snap(FilterSettings(blockUnknown = true))
        assertEquals(BlockReason.UNKNOWN, engine.decide(call("9000000001"), s).reason())
    }

    @Test fun hiddenNumbersOnlyBlockedWhenEnabled() {
        assertTrue(engine.decide(call(null), snap()) is Decision.Allow)
        val s = snap(FilterSettings(blockHidden = true))
        assertEquals(BlockReason.HIDDEN, engine.decide(call(null), s).reason())
    }

    @Test fun spoofedCallersAreBlockedByDefault() {
        assertEquals(BlockReason.SPOOFED, engine.decide(call("9000000001", spoofed = true), snap()).reason())
        val off = snap(FilterSettings(blockSpoofed = false))
        assertTrue(engine.decide(call("9000000001", spoofed = true), off) is Decision.Allow)
    }

    @Test fun spamIsBlockedWithSignals() {
        val d = engine.decide(call("1409876543"), snap()) as Decision.Block
        assertEquals(BlockReason.SPAM, d.reason)
        assertTrue(d.score >= 55)
        assertTrue(d.signals.isNotEmpty())
    }

    @Test fun sensitivityChangesOutcome() {
        // Repeated digits alone score 45: blocked on Strict (35), allowed on Balanced (55).
        val number = "9999999999"
        val strict = snap(FilterSettings(sensitivity = Sensitivity.STRICT))
        val balanced = snap(FilterSettings(sensitivity = Sensitivity.BALANCED))
        assertEquals(BlockReason.SPAM, engine.decide(call(number), strict).reason())
        assertTrue(engine.decide(call(number), balanced) is Decision.Allow)
    }

    @Test fun quietHoursBlockStrangersButNotContacts() {
        val s = snap(FilterSettings(quietHoursEnabled = true, quietStartMinutes = 22 * 60, quietEndMinutes = 7 * 60))
        assertEquals(BlockReason.SCHEDULE, engine.decide(call("9000000001", minute = 23 * 60), s).reason())
        assertEquals(BlockReason.SCHEDULE, engine.decide(call("9000000001", minute = 3 * 60), s).reason())
        assertTrue(engine.decide(call("9000000001", minute = 12 * 60), s) is Decision.Allow)
        assertTrue(engine.decide(call("9811111111", minute = 23 * 60), s) is Decision.Allow)
    }

    @Test fun quietHoursWindowMath() {
        assertTrue(BlockEngine.inQuietHours(60, 22 * 60, 7 * 60))
        assertFalse(BlockEngine.inQuietHours(12 * 60, 22 * 60, 7 * 60))
        assertTrue(BlockEngine.inQuietHours(13 * 60, 12 * 60, 14 * 60))
        assertFalse(BlockEngine.inQuietHours(14 * 60, 12 * 60, 14 * 60))
        assertFalse(BlockEngine.inQuietHours(5, 600, 600))
    }
}

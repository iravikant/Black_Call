package com.blackcall.domain

/**
 * Pure decision logic with no Android dependencies, so it is unit-testable.
 *
 * Order matters (first match wins):
 *  1. emergency numbers            -> always allowed
 *  2. protection off               -> allowed
 *  3. allowed list                 -> allowed
 *  4. blocked list / prefix rules  -> blocked
 *  5. saved contacts               -> allowed
 *  6. quiet hours                  -> blocked
 *  7. contacts-only mode           -> blocked
 *  8. hidden / spoofed / unknown   -> blocked when the filter is on
 *  9. spam score >= sensitivity    -> blocked
 */
class BlockEngine(private val isContact: (String) -> Boolean) {

    fun decide(call: IncomingCall, snapshot: RuleSnapshot): Decision {
        val s = snapshot.settings
        if (call.isEmergency) return Decision.Allow("emergency")
        if (!s.protectionEnabled) return Decision.Allow("disabled")

        val raw = call.rawNumber?.takeIf { it.isNotBlank() }
        val key = raw?.let(PhoneNumbers::key).orEmpty()

        if (key.isNotEmpty() && key in snapshot.allowed) return Decision.Allow("allowed")
        if (key.isNotEmpty() && key in snapshot.blocked) return Decision.Block(BlockReason.MANUAL)
        if (raw != null && snapshot.prefixes.any { PhoneNumbers.matchesPrefix(raw, it) }) {
            return Decision.Block(BlockReason.PREFIX)
        }

        val contact = raw != null && isContact(raw)
        if (contact) return Decision.Allow("contact")

        if (s.quietHoursEnabled && inQuietHours(call.minuteOfDay, s.quietStartMinutes, s.quietEndMinutes)) {
            return Decision.Block(BlockReason.SCHEDULE)
        }
        if (s.contactsOnly) return Decision.Block(BlockReason.NOT_ALLOWED)
        if (raw == null && s.blockHidden) return Decision.Block(BlockReason.HIDDEN)
        if (call.isSpoofed && s.blockSpoofed) return Decision.Block(BlockReason.SPOOFED)
        if (s.blockUnknown) {
            return Decision.Block(if (raw == null) BlockReason.HIDDEN else BlockReason.UNKNOWN)
        }

        if (s.blockSpam && raw != null) {
            val a = SpamScorer.assess(raw, call.callerLabel)
            if (a.score >= s.sensitivity.threshold) {
                return Decision.Block(BlockReason.SPAM, a.score, a.signals)
            }
        }
        return Decision.Allow("clean")
    }

    companion object {
        /** Handles windows that wrap past midnight (e.g. 22:00 -> 07:00). */
        fun inQuietHours(now: Int, start: Int, end: Int): Boolean = when {
            start == end -> false
            start < end -> now in start until end
            else -> now >= start || now < end
        }
    }
}

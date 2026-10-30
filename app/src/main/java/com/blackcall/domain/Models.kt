package com.blackcall.domain

/** Why a call was blocked. Stored in the call log, so keep names stable. */
enum class BlockReason(val label: String, val description: String) {
    MANUAL("Blocked list", "You blocked this number"),
    PREFIX("Prefix rule", "Matches a prefix you blocked"),
    SCHEDULE("Quiet hours", "Received during your quiet hours"),
    NOT_ALLOWED("Contacts only", "Not in your contacts or allowed list"),
    HIDDEN("Hidden number", "The caller hid their number"),
    SPOOFED("Spoofed caller", "Your carrier could not verify this number"),
    UNKNOWN("Unknown caller", "Number is not in your contacts"),
    SPAM("Suspected spam", "Matched known spam patterns")
}

/** What Black Call does with a blocked call. */
enum class BlockAction(val label: String) {
    REJECT("Reject"),
    SILENCE("Silence")
}

enum class RuleType { BLOCK, ALLOW }

enum class Sensitivity(val label: String, val threshold: Int) {
    RELAXED("Relaxed", 75),
    BALANCED("Balanced", 55),
    STRICT("Strict", 35)
}

sealed interface Decision {
    /** [why] is a short machine-readable tag such as "contact" or "emergency". */
    data class Allow(val why: String) : Decision

    data class Block(
        val reason: BlockReason,
        val score: Int = 0,
        val signals: List<String> = emptyList()
    ) : Decision
}

data class FilterSettings(
    val protectionEnabled: Boolean = true,
    val contactsOnly: Boolean = false,
    val blockUnknown: Boolean = false,
    val blockHidden: Boolean = false,
    val blockSpoofed: Boolean = true,
    val blockSpam: Boolean = true,
    val sensitivity: Sensitivity = Sensitivity.BALANCED,
    val action: BlockAction = BlockAction.REJECT,
    val quietHoursEnabled: Boolean = false,
    val quietStartMinutes: Int = 22 * 60,
    val quietEndMinutes: Int = 7 * 60,
    val notifyOnBlock: Boolean = true
)

/** Everything the engine needs, as an immutable in-memory snapshot. */
data class RuleSnapshot(
    val settings: FilterSettings = FilterSettings(),
    /** Normalized keys, see [PhoneNumbers.key]. */
    val blocked: Set<String> = emptySet(),
    val allowed: Set<String> = emptySet(),
    /** Prefixes as stored, e.g. "140" or "+92". */
    val prefixes: List<String> = emptyList()
)

data class IncomingCall(
    /** Null when the caller hid their number. */
    val rawNumber: String?,
    val callerLabel: String? = null,
    val isEmergency: Boolean = false,
    /** True when STIR/SHAKEN verification failed (Android 11+). */
    val isSpoofed: Boolean = false,
    val minuteOfDay: Int = 0
)

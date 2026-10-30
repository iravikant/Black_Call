package com.blackcall.domain

object PhoneNumbers {

    /**
     * Comparison key: digits only, last 10 digits when longer.
     * "+91 98765-43210" and "098765 43210" resolve to the same key.
     */
    fun key(raw: String): String {
        val digits = raw.filter { it.isDigit() }
        return if (digits.length > 10) digits.takeLast(10) else digits
    }

    fun isValidNumber(raw: String): Boolean = raw.count { it.isDigit() } in 3..15

    /** Prefix as stored: optional leading '+', then 1-6 digits. Returns null when invalid. */
    fun normalizePrefix(raw: String): String? {
        val t = raw.trim().replace(" ", "").replace("-", "")
        val plus = t.startsWith("+")
        val digits = t.removePrefix("+")
        if (digits.isEmpty() || digits.length > 6 || !digits.all { it.isDigit() }) return null
        return if (plus) "+$digits" else digits
    }

    /** "+92" matches only international numbers; "140" matches national numbers. */
    fun matchesPrefix(rawNumber: String, prefix: String): Boolean {
        val cleaned = rawNumber.filter { it.isDigit() || it == '+' }
        if (cleaned.isEmpty()) return false
        return if (prefix.startsWith("+")) {
            cleaned.startsWith(prefix)
        } else {
            cleaned.removePrefix("+").startsWith(prefix) && !cleaned.startsWith("+")
        }
    }

    /** Human-friendly formatting for display only. */
    fun display(raw: String?): String {
        if (raw.isNullOrBlank()) return "Hidden number"
        val digits = raw.filter { it.isDigit() }
        return when {
            raw.startsWith("+") && digits.length > 10 -> {
                val cc = digits.dropLast(10)
                val rest = digits.takeLast(10)
                "+$cc ${rest.take(5)} ${rest.drop(5)}"
            }
            digits.length == 10 -> "${digits.take(5)} ${digits.drop(5)}"
            else -> raw.trim()
        }
    }
}

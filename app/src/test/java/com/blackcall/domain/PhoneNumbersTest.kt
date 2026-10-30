package com.blackcall.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PhoneNumbersTest {

    @Test fun keyIgnoresFormattingAndCountryCode() {
        assertEquals("9876543210", PhoneNumbers.key("+91 98765-43210"))
        assertEquals("9876543210", PhoneNumbers.key("098765 43210"))
        assertEquals("9876543210", PhoneNumbers.key("(987) 654-3210"))
    }

    @Test fun shortNumbersAreKeptWhole() {
        assertEquals("112", PhoneNumbers.key("112"))
    }

    @Test fun prefixNormalization() {
        assertEquals("140", PhoneNumbers.normalizePrefix(" 140 "))
        assertEquals("+92", PhoneNumbers.normalizePrefix("+92"))
        assertNull(PhoneNumbers.normalizePrefix(""))
        assertNull(PhoneNumbers.normalizePrefix("abc"))
        assertNull(PhoneNumbers.normalizePrefix("1234567"))
    }

    @Test fun nationalPrefixMatchesNationalNumbersOnly() {
        assertTrue(PhoneNumbers.matchesPrefix("1409876543", "140"))
        assertFalse(PhoneNumbers.matchesPrefix("9140123456", "140"))
        assertFalse(PhoneNumbers.matchesPrefix("+911409876543", "140"))
    }

    @Test fun internationalPrefixMatchesPlusNumbers() {
        assertTrue(PhoneNumbers.matchesPrefix("+923001234567", "+92"))
        assertFalse(PhoneNumbers.matchesPrefix("923001234567", "+92"))
        assertFalse(PhoneNumbers.matchesPrefix("+913001234567", "+92"))
    }

    @Test fun validation() {
        assertTrue(PhoneNumbers.isValidNumber("98765 43210"))
        assertFalse(PhoneNumbers.isValidNumber("12"))
        assertFalse(PhoneNumbers.isValidNumber("1234567890123456"))
    }

    @Test fun displayFormatting() {
        assertEquals("Hidden number", PhoneNumbers.display(null))
        assertEquals("98765 43210", PhoneNumbers.display("9876543210"))
        assertEquals("+91 98765 43210", PhoneNumbers.display("+919876543210"))
    }
}

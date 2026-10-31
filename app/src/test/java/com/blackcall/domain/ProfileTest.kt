package com.blackcall.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class ProfileTest {
    @Test fun initialsFromTwoWords() = assertEquals("RK", Profile("ravi kant").initials())
    @Test fun initialsFromOneWord() = assertEquals("A", Profile("  Asha ").initials())
    @Test fun initialsIgnoreExtraWordsAndSymbols() = assertEquals("JD", Profile("John  Q. Doe").initials())
    @Test fun emptyNameHasNoInitials() = assertEquals("", Profile().initials())
    @Test fun appearanceDefaultsAreSafe() {
        val a = Appearance()
        assertEquals(ThemeMode.DARK, a.mode)
        assertEquals(AccentId.EMERALD, a.accent)
    }
}

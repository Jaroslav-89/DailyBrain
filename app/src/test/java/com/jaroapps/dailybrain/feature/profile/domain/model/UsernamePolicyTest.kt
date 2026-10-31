package com.jaroapps.dailybrain.feature.profile.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UsernamePolicyTest {
    @Test
    fun normalizeRemovesAtAndIgnoresCase() {
        assertEquals("daily_brain7", UsernamePolicy.normalize("@Daily_Brain7"))
    }

    @Test
    fun acceptsOnlyThreeToTwentyLatinLettersDigitsAndUnderscores() {
        assertTrue(UsernamePolicy.isValid("brain_007"))
        assertTrue(UsernamePolicy.isValid("abc"))
        assertTrue(UsernamePolicy.isValid("a".repeat(20)))
        assertFalse(UsernamePolicy.isValid("ab"))
        assertFalse(UsernamePolicy.isValid("a".repeat(21)))
        assertFalse(UsernamePolicy.isValid("имя"))
        assertFalse(UsernamePolicy.isValid("brain.user"))
        assertFalse(UsernamePolicy.isValid("brain user"))
    }

    @Test
    fun sanitizeInputRemovesUnsupportedCharactersAndCapsLength() {
        assertEquals("Brain_007", UsernamePolicy.sanitizeInput("@Brain_007!"))
        assertEquals(20, UsernamePolicy.sanitizeInput("a".repeat(30)).length)
    }
}

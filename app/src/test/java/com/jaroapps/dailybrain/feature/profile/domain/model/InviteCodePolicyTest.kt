package com.jaroapps.dailybrain.feature.profile.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InviteCodePolicyTest {
    @Test
    fun generatedCodesAreEightCharactersAndValid() {
        repeat(100) {
            val code = InviteCodePolicy.generate()
            assertEquals(8, code.length)
            assertTrue(InviteCodePolicy.isValid(code))
        }
    }

    @Test
    fun sanitizesAndNormalizesCode() {
        assertEquals("ABCD2345", InviteCodePolicy.sanitizeInput("ab-cd 2345!"))
    }

    @Test
    fun qrPayloadRoundTripsInviteCode() {
        val code = "ABCD2345"
        assertEquals(code, InviteCodePolicy.fromQrPayload(InviteCodePolicy.qrPayload(code)))
        assertFalse(InviteCodePolicy.isValid("BAD-CODE"))
    }
}

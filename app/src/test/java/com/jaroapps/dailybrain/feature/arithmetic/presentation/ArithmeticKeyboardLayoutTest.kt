package com.jaroapps.dailybrain.feature.arithmetic.presentation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ArithmeticKeyboardLayoutTest {

    @Test
    fun rightHandedKeyboardKeepsDeleteLeftAndSubmitRight() {
        assertEquals(
            listOf("delete", "0", "submit"),
            arithmeticKeyboardRows(isLeftHanded = false).last(),
        )
    }

    @Test
    fun leftHandedKeyboardSwapsDeleteAndSubmitPositions() {
        assertEquals(
            listOf("submit", "0", "delete"),
            arithmeticKeyboardRows(isLeftHanded = true).last(),
        )
    }

    @Test
    fun handednessDoesNotChangeDigitRows() {
        assertEquals(
            arithmeticKeyboardRows(isLeftHanded = false).take(3),
            arithmeticKeyboardRows(isLeftHanded = true).take(3),
        )
    }
}

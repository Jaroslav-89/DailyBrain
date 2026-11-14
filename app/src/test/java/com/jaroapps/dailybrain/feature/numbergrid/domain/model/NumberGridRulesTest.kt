package com.jaroapps.dailybrain.feature.numbergrid.domain.model

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NumberGridRulesTest {

    @Test
    fun `five by five grid contains every number exactly once`() {
        val numbers = NumberGridRules.generateNumbers(5, Random(42))

        assertEquals(25, numbers.size)
        assertEquals((1..25).toList(), numbers.sorted())
    }

    @Test
    fun `generation supports three by three for future difficulty levels`() {
        val numbers = NumberGridRules.generateNumbers(3, Random(7))

        assertEquals(9, numbers.size)
        assertEquals((1..9).toList(), numbers.sorted())
    }

    @Test
    fun `only the expected number is accepted`() {
        assertTrue(NumberGridRules.isCorrectSelection(1, 1))
        assertFalse(NumberGridRules.isCorrectSelection(3, 2))
    }

    @Test
    fun `one error in five by five reduces accuracy by four percentage points`() {
        assertEquals(96.0, NumberGridRules.accuracyPercent(1, 25), 0.0001)
        assertEquals(92.0, NumberGridRules.accuracyPercent(2, 25), 0.0001)
        assertEquals(100.0, NumberGridRules.accuracyPercent(0, 25), 0.0001)
    }

    @Test
    fun `accuracy is clamped at zero and negative errors do not increase it`() {
        assertEquals(0.0, NumberGridRules.accuracyPercent(30, 25), 0.0001)
        assertEquals(100.0, NumberGridRules.accuracyPercent(-1, 25), 0.0001)
    }
}

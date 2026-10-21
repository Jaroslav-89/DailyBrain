package com.jaroapps.dailybrain.common.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExerciseTypeTest {

    @Test
    fun `every exercise type round trips through its storage value`() {
        ExerciseType.entries.forEach { type ->
            assertEquals(type, ExerciseType.fromStorage(type.storageValue))
        }
    }

    @Test
    fun `unknown storage value returns null`() {
        assertNull(ExerciseType.fromStorage("UNKNOWN_EXERCISE"))
    }

    @Test
    fun `storage values remain stable identifiers`() {
        assertEquals("ARITHMETIC", ExerciseType.ARITHMETIC.storageValue)
        assertEquals("MEMORY_WORDS", ExerciseType.MEMORY_WORDS.storageValue)
        assertEquals("COUNTING_TRAINING", ExerciseType.COUNTING_TRAINING.storageValue)
        assertEquals("STROOP_TEST", ExerciseType.STROOP_TEST.storageValue)
    }
}

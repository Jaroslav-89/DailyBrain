package com.jaroapps.dailybrain.common.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class StreakPatchRulesTest {
    private val monday = LocalDate.of(2026, 11, 2)

    @Test
    fun enoughPatchesProtectEveryMissedDayAndKeepTheSeriesActive() {
        val result = StreakPatchRules.process(
            lastProcessedDate = monday,
            throughDate = monday.plusDays(3),
            trainingDates = setOf(monday),
            existingPatchedDates = emptySet(),
            isStreakActive = true,
            availablePatches = 3,
        )

        assertEquals(listOf(monday.plusDays(1), monday.plusDays(2), monday.plusDays(3)), result.newlyPatchedDates)
        assertEquals(monday.plusDays(3), result.lastProcessedDate)
        assertTrue(result.isStreakActive)
    }

    @Test
    fun insufficientPatchesProtectOnlyTheFirstMissedDays() {
        val result = StreakPatchRules.process(
            lastProcessedDate = monday,
            throughDate = monday.plusDays(4),
            trainingDates = emptySet(),
            existingPatchedDates = emptySet(),
            isStreakActive = true,
            availablePatches = 2,
        )

        assertEquals(listOf(monday.plusDays(1), monday.plusDays(2)), result.newlyPatchedDates)
        assertFalse(result.isStreakActive)
    }

    @Test
    fun laterTrainingStartsANewRunButDoesNotRepairEarlierGap() {
        val result = StreakPatchRules.process(
            lastProcessedDate = monday,
            throughDate = monday.plusDays(4),
            trainingDates = setOf(monday.plusDays(3)),
            existingPatchedDates = emptySet(),
            isStreakActive = true,
            availablePatches = 1,
        )

        assertEquals(listOf(monday.plusDays(1)), result.newlyPatchedDates)
        assertFalse(result.isStreakActive)
    }

    @Test
    fun existingPatchDaysAreNotChargedTwice() {
        val result = StreakPatchRules.process(
            lastProcessedDate = monday,
            throughDate = monday.plusDays(2),
            trainingDates = setOf(monday),
            existingPatchedDates = setOf(monday.plusDays(1)),
            isStreakActive = true,
            availablePatches = 2,
        )

        assertTrue(result.newlyPatchedDates.isEmpty())
        assertTrue(result.isStreakActive)
    }

    @Test
    fun aBrokenRunDoesNotSpendPatchesUntilAnotherWorkoutOccurs() {
        val result = StreakPatchRules.process(
            lastProcessedDate = monday,
            throughDate = monday.plusDays(4),
            trainingDates = setOf(monday.plusDays(2)),
            existingPatchedDates = emptySet(),
            isStreakActive = false,
            availablePatches = 5,
        )

        assertEquals(listOf(monday.plusDays(3), monday.plusDays(4)), result.newlyPatchedDates)
        assertTrue(result.isStreakActive)
    }

    @Test
    fun datesBeforeProcessingCursorAreNeverRevisited() {
        val result = StreakPatchRules.process(
            lastProcessedDate = monday.plusDays(4),
            throughDate = monday.plusDays(2),
            trainingDates = setOf(monday),
            existingPatchedDates = emptySet(),
            isStreakActive = false,
            availablePatches = 5,
        )

        assertTrue(result.newlyPatchedDates.isEmpty())
        assertEquals(monday.plusDays(4), result.lastProcessedDate)
    }
}

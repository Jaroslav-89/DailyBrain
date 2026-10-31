package com.jaroapps.dailybrain.feature.home.presentation

import com.jaroapps.dailybrain.common.domain.model.ExerciseType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class HomeUiStateTest {
    @Test
    fun weekAlwaysStartsOnMonday() {
        val state = HomeUiState(today = LocalDate.of(2026, 10, 27))

        assertEquals(LocalDate.of(2026, 10, 26), state.currentWeekStart)
        assertEquals(LocalDate.of(2026, 11, 1), state.currentWeekDates.last())
        assertEquals(7, state.currentWeekDates.size)
    }

    @Test
    fun currentStreakIncludesTodayWhenTrainingIsCompletedToday() {
        val today = LocalDate.of(2026, 10, 27)
        val state = HomeUiState(
            today = today,
            completedDates = setOf(today, today.minusDays(1), today.minusDays(2)),
        )

        assertEquals(3, state.currentStreakDays)
    }

    @Test
    fun currentStreakCanContinueUntilTheEndOfAnUnfinishedDay() {
        val today = LocalDate.of(2026, 10, 27)
        val state = HomeUiState(
            today = today,
            completedDates = setOf(today.minusDays(1), today.minusDays(2)),
        )

        assertEquals(2, state.currentStreakDays)
    }

    @Test
    fun exerciseCompletionIsScopedToCurrentWeek() {
        val today = LocalDate.of(2026, 10, 27)
        val state = HomeUiState(
            today = today,
            completedExerciseDatesByType = mapOf(
                ExerciseType.MEMORY_WORDS.storageValue to setOf(today.minusDays(7)),
                ExerciseType.STROOP_TEST.storageValue to setOf(today.minusDays(1)),
            ),
        )

        assertFalse(state.hasCompletedExerciseThisWeek(ExerciseType.MEMORY_WORDS))
        assertTrue(state.hasCompletedExerciseThisWeek(ExerciseType.STROOP_TEST))
    }

    @Test
    fun onlyTodayIsAvailableAsAnArithmeticDay() {
        val today = LocalDate.of(2026, 10, 27)
        val state = HomeUiState(today = today)

        assertEquals(ArithmeticDayStatus.AVAILABLE_TODAY, state.arithmeticDayStatus(today))
        assertEquals(ArithmeticDayStatus.MISSED, state.arithmeticDayStatus(today.minusDays(1)))
        assertEquals(ArithmeticDayStatus.FUTURE, state.arithmeticDayStatus(today.plusDays(1)))
    }
}

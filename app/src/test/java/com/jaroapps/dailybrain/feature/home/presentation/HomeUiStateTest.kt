package com.jaroapps.dailybrain.feature.home.presentation

import com.jaroapps.dailybrain.common.domain.model.ExerciseType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class HomeUiStateTest {
    @Test
    fun daysBeforeFirstTrainingAreGrayRatherThanMissed() {
        val monday = LocalDate.of(2026, 10, 26)
        val wednesday = monday.plusDays(2)
        val today = LocalDate.of(2026, 11, 2)
        val state = HomeUiState(
            today = today,
            completedDates = setOf(wednesday, wednesday.plusDays(1), today),
            completedExerciseDatesByType = mapOf(
                ExerciseType.ARITHMETIC.storageValue to setOf(today),
            ),
            isTrainingHistoryLoaded = true,
        )

        assertEquals(ArithmeticDayStatus.BEFORE_FIRST_TRAINING, state.arithmeticDayStatus(monday))
        assertEquals(ArithmeticDayStatus.BEFORE_FIRST_TRAINING, state.arithmeticDayStatus(monday.plusDays(1)))
        assertEquals(ArithmeticDayStatus.MISSED, state.arithmeticDayStatus(wednesday.plusDays(1)))
        assertEquals(ArithmeticDayStatus.COMPLETED, state.arithmeticDayStatus(today))
    }

    @Test
    fun noTrainingHistoryTreatsPastDaysAsBeforeFirstTraining() {
        val today = LocalDate.of(2026, 11, 2)
        val state = HomeUiState(today = today, isTrainingHistoryLoaded = true)

        assertEquals(ArithmeticDayStatus.BEFORE_FIRST_TRAINING, state.arithmeticDayStatus(today.minusDays(1)))
        assertEquals(ArithmeticDayStatus.AVAILABLE_TODAY, state.arithmeticDayStatus(today))
    }
}

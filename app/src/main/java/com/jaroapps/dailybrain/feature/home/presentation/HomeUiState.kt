package com.jaroapps.dailybrain.feature.home.presentation

import com.jaroapps.dailybrain.common.domain.model.ExerciseType
import com.jaroapps.dailybrain.common.domain.model.TrainingStreakCalculator
import java.time.DayOfWeek
import java.time.LocalDate

enum class ArithmeticDayStatus {
    LOADING,
    FUTURE,
    AVAILABLE_TODAY,
    COMPLETED,
    MISSED,
}

data class HomeUiState(
    val today: LocalDate = LocalDate.now(),
    val completedDates: Set<LocalDate> = emptySet(),
    val completedExerciseDatesByType: Map<String, Set<LocalDate>> = emptyMap(),
    val isTrainingHistoryLoaded: Boolean = false,
    val totalTrainingCount: Int = 0,
    val xpPoints: Int = 0,
    val endorphinBalance: Int = 0,
) {
    val isTrainingCompletedToday: Boolean
        get() = today in completedDates

    val currentWeekStart: LocalDate
        get() = today.with(DayOfWeek.MONDAY)

    val currentWeekDates: List<LocalDate>
        get() = (0..6).map { currentWeekStart.plusDays(it.toLong()) }

    val currentStreakDays: Int
        get() = TrainingStreakCalculator.calculate(completedDates, today).currentDays

    fun arithmeticDayStatus(date: LocalDate): ArithmeticDayStatus = when {
        date.isAfter(today) -> ArithmeticDayStatus.FUTURE
        !isTrainingHistoryLoaded -> ArithmeticDayStatus.LOADING
        date in completedExerciseDatesByType[ExerciseType.ARITHMETIC.storageValue].orEmpty() ->
            ArithmeticDayStatus.COMPLETED
        date.isBefore(today) -> ArithmeticDayStatus.MISSED
        else -> ArithmeticDayStatus.AVAILABLE_TODAY
    }

    fun hasCompletedExerciseThisWeek(type: ExerciseType): Boolean =
        completedExerciseDatesByType[type.storageValue].orEmpty().any { date ->
            date in currentWeekStart..today
        }
}

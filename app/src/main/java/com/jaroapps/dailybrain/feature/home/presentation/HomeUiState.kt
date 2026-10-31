package com.jaroapps.dailybrain.feature.home.presentation

import com.jaroapps.dailybrain.common.domain.model.ExerciseType
import java.time.DayOfWeek
import java.time.LocalDate

enum class ArithmeticDayStatus {
    FUTURE,
    AVAILABLE_TODAY,
    COMPLETED,
    MISSED,
}

data class HomeUiState(
    val today: LocalDate = LocalDate.now(),
    val completedDates: Set<LocalDate> = emptySet(),
    val completedExerciseDatesByType: Map<String, Set<LocalDate>> = emptyMap(),
    val totalTrainingCount: Int = 0,
) {
    val isTrainingCompletedToday: Boolean
        get() = today in completedDates

    val currentWeekStart: LocalDate
        get() = today.with(DayOfWeek.MONDAY)

    val currentWeekDates: List<LocalDate>
        get() = (0..6).map { currentWeekStart.plusDays(it.toLong()) }

    val currentStreakDays: Int
        get() {
            var cursor = if (today in completedDates) today else today.minusDays(1)
            var count = 0
            while (cursor in completedDates) {
                count++
                cursor = cursor.minusDays(1)
            }
            return count
        }

    fun arithmeticDayStatus(date: LocalDate): ArithmeticDayStatus = when {
        date.isAfter(today) -> ArithmeticDayStatus.FUTURE
        date in completedDates -> ArithmeticDayStatus.COMPLETED
        date.isBefore(today) -> ArithmeticDayStatus.MISSED
        else -> ArithmeticDayStatus.AVAILABLE_TODAY
    }

    fun hasCompletedExerciseThisWeek(type: ExerciseType): Boolean =
        completedExerciseDatesByType[type.storageValue].orEmpty().any { date ->
            date in currentWeekStart..today
        }
}

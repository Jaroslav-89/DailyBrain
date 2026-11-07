package com.jaroapps.dailybrain.feature.home.presentation

import com.jaroapps.dailybrain.common.domain.model.ExerciseType
import com.jaroapps.dailybrain.common.domain.model.TrainingStreakCalculator
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

enum class ArithmeticDayStatus {
    LOADING,
    FUTURE,
    AVAILABLE_TODAY,
    COMPLETED,
    PATCHED,
    BEFORE_FIRST_TRAINING,
    MISSED,
}

data class HomeUiState(
    val today: LocalDate = LocalDate.now(),
    /** Real training days combined with days protected by a patch for streak counting. */
    val completedDates: Set<LocalDate> = emptySet(),
    val patchedDates: Set<LocalDate> = emptySet(),
    val completedExerciseDatesByType: Map<String, Set<LocalDate>> = emptyMap(),
    val isTrainingHistoryLoaded: Boolean = false,
    val totalTrainingCount: Int = 0,
    val xpPoints: Int = 0,
    val endorphinBalance: Int = 0,
) {
    val isTrainingCompletedToday: Boolean
        get() = today in completedDates && today !in patchedDates

    val currentWeekStart: LocalDate
        get() = today.with(DayOfWeek.MONDAY)

    val currentWeekDates: List<LocalDate>
        get() = weekDates(currentWeekStart)

    /** First real completed exercise date; protected days never define the beginning of history. */
    val firstTrainingDate: LocalDate?
        get() = completedExerciseDatesByType.values.asSequence().flatten().minOrNull()

    val firstTrainingWeekStart: LocalDate
        get() = firstTrainingDate?.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            ?: currentWeekStart

    /** The pager includes every calendar week from the first real workout through the current one. */
    val weekPageCount: Int
        get() = ChronoUnit.WEEKS.between(firstTrainingWeekStart, currentWeekStart)
            .toInt()
            .coerceAtLeast(0) + 1

    val currentWeekPageIndex: Int
        get() = weekPageCount - 1

    val currentStreakDays: Int
        get() = TrainingStreakCalculator.calculate(completedDates, today).currentDays

    fun weekStartAtPage(page: Int): LocalDate {
        require(page in 0 until weekPageCount)
        return firstTrainingWeekStart.plusWeeks(page.toLong())
    }

    fun weekDates(weekStart: LocalDate = currentWeekStart): List<LocalDate> =
        (0..6).map { weekStart.plusDays(it.toLong()) }

    fun arithmeticDayStatus(date: LocalDate): ArithmeticDayStatus = when {
        date.isAfter(today) -> ArithmeticDayStatus.FUTURE
        !isTrainingHistoryLoaded -> ArithmeticDayStatus.LOADING
        date in patchedDates -> ArithmeticDayStatus.PATCHED
        date in completedExerciseDatesByType[ExerciseType.ARITHMETIC.storageValue].orEmpty() ->
            ArithmeticDayStatus.COMPLETED
        date.isBefore(firstTrainingDate ?: today) -> ArithmeticDayStatus.BEFORE_FIRST_TRAINING
        date.isBefore(today) -> ArithmeticDayStatus.MISSED
        else -> ArithmeticDayStatus.AVAILABLE_TODAY
    }

    fun hasCompletedExerciseThisWeek(
        type: ExerciseType,
        weekStart: LocalDate = currentWeekStart,
    ): Boolean {
        val weekEnd = weekStart.plusDays(6)
        return completedExerciseDatesByType[type.storageValue].orEmpty().any { date ->
            date in weekStart..weekEnd
        }
    }
}

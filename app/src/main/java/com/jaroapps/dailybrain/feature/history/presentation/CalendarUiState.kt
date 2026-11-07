package com.jaroapps.dailybrain.feature.history.presentation

import com.jaroapps.dailybrain.common.domain.model.TrainingStreakCalculator
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import kotlin.math.ceil

data class WeeklyChallengeConditionUiState(
    val exerciseType: String,
    val title: String,
    val completedCount: Int,
    val requiredCount: Int,
) {
    val progress: Float
        get() = (completedCount.toFloat() / requiredCount.coerceAtLeast(1)).coerceIn(0f, 1f)

    val isCompleted: Boolean
        get() = completedCount >= requiredCount
}

data class CalendarUiState(
    val displayedMonth: YearMonth = YearMonth.now(),
    /** Days with at least one real, completed exercise. */
    val activeDates: Set<LocalDate> = emptySet(),
    /** Days protected by an automatically consumed patch. */
    val patchedDates: Set<LocalDate> = emptySet(),
    val firstTrainingDate: LocalDate? = null,
    val weeklyChallengeConditions: List<WeeklyChallengeConditionUiState> = emptyList(),
    val currentTime: LocalDateTime = LocalDateTime.now(),
) {
    val today: LocalDate
        get() = currentTime.toLocalDate()

    val hasTrainingToday: Boolean
        get() = today in activeDates

    val streakDates: Set<LocalDate>
        get() = activeDates + patchedDates

    val currentStreakDays: Int
        get() = TrainingStreakCalculator.calculate(streakDates, today).currentDays

    /** Progress through the current rolling seven-day achievement segment. */
    val weeklyStreakProgress: Int
        get() = if (currentStreakDays == 0) 0 else ((currentStreakDays - 1) % 7) + 1

    /** Number of active or patch-protected dates so far in the current calendar month. */
    val monthlyStreakProgress: Int
        get() {
            val month = YearMonth.from(today)
            var consecutiveDays = 0
            for (dayNumber in 1..today.dayOfMonth) {
                if (month.atDay(dayNumber) in streakDates) consecutiveDays++ else break
            }
            return consecutiveDays
        }

    val monthlyStreakTarget: Int
        get() = YearMonth.from(today).lengthOfMonth()

    val isWeeklyChallengeCompleted: Boolean
        get() = weeklyChallengeConditions.isNotEmpty() && weeklyChallengeConditions.all { it.isCompleted }

    val hoursRemainingToday: Int
        get() {
            val midnight = today.plusDays(1).atStartOfDay()
            val minutes = Duration.between(currentTime, midnight).toMinutes().coerceAtLeast(0)
            return ceil(minutes / 60.0).toInt()
        }
}

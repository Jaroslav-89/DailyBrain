package com.jaroapps.dailybrain.feature.history.presentation

import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import kotlin.math.ceil

data class CalendarUiState(
    val displayedMonth: YearMonth = YearMonth.now(),
    val activeDates: Set<LocalDate> = emptySet(),
    val firstTrainingDate: LocalDate? = null,
    val currentTime: LocalDateTime = LocalDateTime.now(),
) {
    val today: LocalDate
        get() = currentTime.toLocalDate()

    val hasTrainingToday: Boolean
        get() = today in activeDates

    val currentStreakDays: Int
        get() {
            var cursor = if (hasTrainingToday) today else today.minusDays(1)
            var count = 0
            while (cursor in activeDates) {
                count++
                cursor = cursor.minusDays(1)
            }
            return count
        }

    val hoursRemainingToday: Int
        get() {
            val midnight = today.plusDays(1).atStartOfDay()
            val minutes = Duration.between(currentTime, midnight).toMinutes().coerceAtLeast(0)
            return ceil(minutes / 60.0).toInt()
        }
}

package com.jaroapps.dailybrain.feature.home.presentation

import java.time.LocalDate

data class DailyActivityUiModel(
    val date: LocalDate,
    val isCompleted: Boolean,
)

data class HomeUiState(
    val today: LocalDate = LocalDate.now(),
    val completedDates: Set<LocalDate> = emptySet(),
    val totalTrainingCount: Int = 0,
) {
    val isTrainingCompletedToday: Boolean
        get() = today in completedDates

    val lastSevenDays: List<DailyActivityUiModel>
        get() = (6 downTo 0).map { daysAgo ->
            val date = today.minusDays(daysAgo.toLong())

            DailyActivityUiModel(
                date = date,
                isCompleted = date in completedDates,
            )
        }
}

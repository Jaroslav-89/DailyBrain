package com.jaroapps.dailybrain.common.domain.model

import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class TrainingStreak(
    val currentDays: Int,
    val bestDays: Int,
)

/**
 * Calculates current and longest training streaks from the persisted calendar-day index.
 *
 * The dates in training_days are authoritative historical local dates. They must not be
 * reconstructed from session timestamps, because the device time zone may have changed.
 */
object TrainingStreakCalculator {
    fun parseDateKeys(dateKeys: Iterable<Int>): Set<LocalDate> =
        dateKeys.mapNotNull { key ->
            runCatching {
                LocalDate.parse(key.toString(), DateTimeFormatter.BASIC_ISO_DATE)
            }.getOrNull()
        }.toSet()

    fun calculate(
        activeDates: Set<LocalDate>,
        today: LocalDate,
    ): TrainingStreak {
        val dates = activeDates.filterNot { it.isAfter(today) }.toSortedSet()

        var currentDays = 0
        var cursor = if (today in dates) today else today.minusDays(1)
        while (cursor in dates) {
            currentDays++
            cursor = cursor.minusDays(1)
        }

        var bestDays = 0
        var previousDate: LocalDate? = null
        var currentRun = 0
        dates.forEach { date ->
            currentRun = if (previousDate?.plusDays(1) == date) currentRun + 1 else 1
            bestDays = maxOf(bestDays, currentRun)
            previousDate = date
        }

        return TrainingStreak(currentDays = currentDays, bestDays = bestDays)
    }
}

package com.jaroapps.dailybrain.feature.history.presentation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

class CalendarUiStateTest {
    @Test
    fun currentStreakStartsFromTodayOrYesterday() {
        val today = LocalDate.of(2026, 10, 27)
        val time = LocalDateTime.of(2026, 10, 27, 12, 0)
        val state = CalendarUiState(
            displayedMonth = YearMonth.from(today),
            activeDates = setOf(today, today.minusDays(1), today.minusDays(2)),
            currentTime = time,
        )

        assertEquals(3, state.currentStreakDays)
        assertTrue(state.hasTrainingToday)
    }

    @Test
    fun patchedDatesCountTowardWeeklyAndMonthlySeriesProgress() {
        val today = LocalDate.of(2026, 10, 27)
        val patchedDate = today.minusDays(1)
        val allMonthDatesSoFar = (1..today.dayOfMonth)
            .map { LocalDate.of(today.year, today.month, it) }
            .toSet()
        val state = CalendarUiState(
            displayedMonth = YearMonth.from(today),
            activeDates = allMonthDatesSoFar - patchedDate,
            patchedDates = setOf(patchedDate),
            currentTime = LocalDateTime.of(2026, 10, 27, 12, 0),
        )

        assertEquals(3, state.currentStreakDays)
        assertEquals(3, state.weeklyStreakProgress)
        assertEquals(27, state.monthlyStreakProgress)
        assertEquals(31, state.monthlyStreakTarget)
        assertTrue(state.hasTrainingToday)
    }

    @Test
    fun firstUnprotectedDayStopsMonthlyStreakProgress() {
        val today = LocalDate.of(2026, 10, 18)
        val dates = (1..10).map { LocalDate.of(2026, 10, it) }.toSet() - LocalDate.of(2026, 10, 5)
        val state = CalendarUiState(
            displayedMonth = YearMonth.from(today),
            activeDates = dates,
            currentTime = LocalDateTime.of(2026, 10, 18, 12, 0),
        )

        assertEquals(4, state.monthlyStreakProgress)
    }

    @Test
    fun weeklyChallengeConditionsExposeProgressAndCompletion() {
        val state = CalendarUiState(
            weeklyChallengeConditions = listOf(
                WeeklyChallengeConditionUiState("ARITHMETIC", "Арифметика", 7, 7),
                WeeklyChallengeConditionUiState("MEMORY_WORDS", "Запоминание слов", 0, 1),
            ),
        )

        assertTrue(state.weeklyChallengeConditions.first().isCompleted)
        assertEquals(1f, state.weeklyChallengeConditions.first().progress)
        assertFalse(state.isWeeklyChallengeCompleted)
    }

    @Test
    fun unfinishedTodayKeepsYesterdayStreakAlive() {
        val today = LocalDate.of(2026, 10, 27)
        val state = CalendarUiState(
            displayedMonth = YearMonth.from(today),
            activeDates = setOf(today.minusDays(1), today.minusDays(2)),
            currentTime = LocalDateTime.of(2026, 10, 27, 12, 0),
        )

        assertEquals(2, state.currentStreakDays)
        assertFalse(state.hasTrainingToday)
        assertEquals(12, state.hoursRemainingToday)
    }
}

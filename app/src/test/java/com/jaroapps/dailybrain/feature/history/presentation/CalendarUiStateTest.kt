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
        val state = CalendarUiState(
            displayedMonth = YearMonth.from(today),
            activeDates = setOf(today, today.minusDays(2)),
            patchedDates = setOf(today.minusDays(1)),
            currentTime = LocalDateTime.of(2026, 10, 27, 12, 0),
        )

        assertEquals(3, state.currentStreakDays)
        assertEquals(3, state.weeklyStreakProgress)
        assertEquals(3, state.monthlyStreakProgress)
        assertEquals(31, state.monthlyStreakTarget)
        assertTrue(state.hasTrainingToday)
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

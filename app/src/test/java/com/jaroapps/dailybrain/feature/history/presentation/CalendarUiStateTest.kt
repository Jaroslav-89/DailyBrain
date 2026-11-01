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

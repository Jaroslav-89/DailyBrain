package com.jaroapps.dailybrain.common.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class TrainingStreakCalculatorTest {
    @Test
    fun currentStreakIncludesTodayWhenAlreadyTrained() {
        val today = LocalDate.of(2026, 11, 1)
        val dates = setOf(today, today.minusDays(1), today.minusDays(2))

        val result = TrainingStreakCalculator.calculate(dates, today)

        assertEquals(3, result.currentDays)
        assertEquals(3, result.bestDays)
    }

    @Test
    fun unfinishedTodayKeepsYesterdayStreakAlive() {
        val today = LocalDate.of(2026, 11, 1)
        val dates = setOf(today.minusDays(1), today.minusDays(2))

        val result = TrainingStreakCalculator.calculate(dates, today)

        assertEquals(2, result.currentDays)
        assertEquals(2, result.bestDays)
    }

    @Test
    fun currentStreakStopsAtMissingDayButBestStreakKeepsHistoricalRecord() {
        val today = LocalDate.of(2026, 11, 1)
        val dates = setOf(
            LocalDate.of(2026, 10, 20),
            LocalDate.of(2026, 10, 21),
            LocalDate.of(2026, 10, 22),
            LocalDate.of(2026, 10, 24),
            LocalDate.of(2026, 10, 25),
            LocalDate.of(2026, 10, 26),
            LocalDate.of(2026, 10, 27),
            LocalDate.of(2026, 10, 29),
            LocalDate.of(2026, 10, 30),
            LocalDate.of(2026, 10, 31),
        )

        val result = TrainingStreakCalculator.calculate(dates, today)

        assertEquals(3, result.currentDays)
        assertEquals(4, result.bestDays)
    }

    @Test
    fun futureDatesDoNotAffectCurrentOrBestStreak() {
        val today = LocalDate.of(2026, 11, 1)
        val dates = setOf(today, today.plusDays(1), today.plusDays(2))

        val result = TrainingStreakCalculator.calculate(dates, today)

        assertEquals(1, result.currentDays)
        assertEquals(1, result.bestDays)
    }

    @Test
    fun parsesPersistedCalendarDateKeysAndIgnoresInvalidKeys() {
        val today = LocalDate.of(2026, 11, 1)
        val dates = TrainingStreakCalculator.parseDateKeys(
            listOf(20261101, 20261031, 20261340, -1),
        )

        assertTrue(today in dates)
        assertEquals(setOf(today, today.minusDays(1)), dates)
    }
}

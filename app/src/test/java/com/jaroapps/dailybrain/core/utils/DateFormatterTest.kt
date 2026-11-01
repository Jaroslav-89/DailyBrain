package com.jaroapps.dailybrain.core.utils

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class DateFormatterTest {

    private val tallinnZone = ZoneId.of("Europe/Tallinn")

    @Test
    fun nightAndDayWorkoutsOnTheSameLocalDateUseTheSameRewardDay() {
        val nightWorkout = Instant.parse("2026-11-01T22:30:00Z").toEpochMilli() // 00:30 local
        val dayWorkout = Instant.parse("2026-11-02T12:00:00Z").toEpochMilli() // 14:00 local

        assertEquals(
            LocalDate.of(2026, 11, 2),
            localDateAtEpochMillis(nightWorkout, tallinnZone),
        )
        assertEquals(
            localDateAtEpochMillis(nightWorkout, tallinnZone),
            localDateAtEpochMillis(dayWorkout, tallinnZone),
        )
    }

    @Test
    fun localRewardDayChangesExactlyAtMidnight() {
        val oneSecondBeforeMidnight = Instant.parse("2026-11-01T21:59:59Z").toEpochMilli()
        val exactMidnight = Instant.parse("2026-11-01T22:00:00Z").toEpochMilli()

        assertEquals(
            LocalDate.of(2026, 11, 1),
            localDateAtEpochMillis(oneSecondBeforeMidnight, tallinnZone),
        )
        assertEquals(
            LocalDate.of(2026, 11, 2),
            localDateAtEpochMillis(exactMidnight, tallinnZone),
        )
    }
}

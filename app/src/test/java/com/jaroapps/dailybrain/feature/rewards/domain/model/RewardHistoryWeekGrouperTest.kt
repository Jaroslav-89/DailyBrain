package com.jaroapps.dailybrain.feature.rewards.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class RewardHistoryWeekGrouperTest {

    private val tallinn = ZoneId.of("Europe/Tallinn")

    @Test
    fun groupsEntriesFromMondayThroughSundayAndCalculatesNetWeeklyChange() {
        val entries = listOf(
            entry("monday", 80, "2026-11-02T08:00:00Z"),
            entry("sunday-spend", -15, "2026-11-08T08:00:00Z"),
            entry("sunday-credit", 20, "2026-11-08T09:00:00Z"),
            entry("next-week", -5, "2026-11-09T08:00:00Z"),
        )

        val weeks = groupRewardHistoryByWeek(entries, tallinn)

        assertEquals(2, weeks.size)
        assertEquals(LocalDate.of(2026, 11, 9), weeks[0].weekStart)
        assertEquals(LocalDate.of(2026, 11, 15), weeks[0].weekEnd)
        assertEquals(-5L, weeks[0].totalAmount)
        assertEquals(listOf("next-week"), weeks[0].entries.map { it.id })

        assertEquals(LocalDate.of(2026, 11, 2), weeks[1].weekStart)
        assertEquals(LocalDate.of(2026, 11, 8), weeks[1].weekEnd)
        assertEquals(85L, weeks[1].totalAmount)
        assertEquals(
            listOf("sunday-credit", "sunday-spend", "monday"),
            weeks[1].entries.map { it.id },
        )
    }

    @Test
    fun assignsWeekUsingLocalCalendarDateAndNotUtcDate() {
        // 22:30 UTC on Sunday is already 00:30 Monday in Tallinn.
        val entry = entry("crosses-midnight", 12, "2026-11-01T22:30:00Z")

        val weeks = groupRewardHistoryByWeek(listOf(entry), tallinn)

        assertEquals(LocalDate.of(2026, 11, 2), weeks.single().weekStart)
        assertEquals(LocalDate.of(2026, 11, 8), weeks.single().weekEnd)
    }

    @Test
    fun weeklyNetChangeCanBeZero() {
        val entries = listOf(
            entry("credit", 50, "2026-11-03T08:00:00Z"),
            entry("spend", -50, "2026-11-04T08:00:00Z"),
        )

        assertEquals(0L, groupRewardHistoryByWeek(entries, tallinn).single().totalAmount)
    }

    private fun entry(id: String, amount: Int, timestamp: String) = RewardHistoryEntry(
        id = id,
        amount = amount,
        reason = "TEST",
        occurredAtEpochMs = Instant.parse(timestamp).toEpochMilli(),
    )
}

package com.jaroapps.dailybrain.feature.rewards.domain.model

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

data class RewardHistoryWeekSection(
    val weekStart: LocalDate,
    val weekEnd: LocalDate,
    val totalAmount: Long,
    val entries: List<RewardHistoryEntry>,
)

/** Groups reward changes into local calendar weeks (Monday through Sunday). */
fun groupRewardHistoryByWeek(
    entries: List<RewardHistoryEntry>,
    zoneId: ZoneId,
): List<RewardHistoryWeekSection> =
    entries.groupBy { entry ->
        Instant.ofEpochMilli(entry.occurredAtEpochMs)
            .atZone(zoneId)
            .toLocalDate()
            .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    }
        .entries
        .sortedByDescending { it.key }
        .map { (weekStart, weekEntries) ->
            RewardHistoryWeekSection(
                weekStart = weekStart,
                weekEnd = weekStart.plusDays(6),
                totalAmount = weekEntries.sumOf { it.amount.toLong() },
                entries = weekEntries.sortedWith(
                    compareByDescending<RewardHistoryEntry> { it.occurredAtEpochMs }
                        .thenByDescending { it.id },
                ),
            )
        }

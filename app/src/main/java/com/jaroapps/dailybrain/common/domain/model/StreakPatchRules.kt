package com.jaroapps.dailybrain.common.domain.model

import java.time.LocalDate

data class StreakPatchProcessingResult(
    val newlyPatchedDates: List<LocalDate>,
    val missedDates: List<LocalDate>,
    val lastProcessedDate: LocalDate,
    val wasStreakActive: Boolean,
    val isStreakActive: Boolean,
    val isPreviousStreakPreserved: Boolean,
)

data class StreakPatchProcessingSummary(
    val missedDayCount: Int = 0,
    val usedPatchCount: Int = 0,
    val remainingPatchCount: Int = 0,
    val wasStreakActive: Boolean = false,
    val isStreakPreserved: Boolean = false,
    val isStreakActiveAfter: Boolean = false,
    val lastProcessedDate: LocalDate? = null,
    val patchesAppliedAtEpochMs: Long? = null,
)

object StreakPatchRules {
    /**
     * Processes dates in chronological order. A missed date ends the current run when no
     * patch is available; later dates stay unprotected until a real workout starts a new run.
     *
     * missedDates includes every date without a workout or an already-applied patch. The
     * preservation flag refers only to the streak that was active before this processing pass;
     * a later workout may start a new streak but cannot retroactively save the previous one.
     */
    fun process(
        lastProcessedDate: LocalDate,
        throughDate: LocalDate,
        trainingDates: Set<LocalDate>,
        existingPatchedDates: Set<LocalDate>,
        isStreakActive: Boolean,
        availablePatches: Int,
    ): StreakPatchProcessingResult {
        var cursor = lastProcessedDate
        var active = isStreakActive
        var remaining = availablePatches.coerceAtLeast(0)
        val wasStreakActive = isStreakActive
        var previousStreakBroken = false
        val newlyPatched = mutableListOf<LocalDate>()
        val missed = mutableListOf<LocalDate>()

        while (cursor.isBefore(throughDate)) {
            cursor = cursor.plusDays(1)
            when {
                cursor in trainingDates -> active = true
                cursor in existingPatchedDates -> active = true
                else -> {
                    missed += cursor
                    if (active && remaining > 0) {
                        newlyPatched += cursor
                        remaining--
                    } else {
                        if (wasStreakActive) previousStreakBroken = true
                        active = false
                    }
                }
            }
        }

        return StreakPatchProcessingResult(
            newlyPatchedDates = newlyPatched,
            missedDates = missed,
            lastProcessedDate = cursor,
            wasStreakActive = wasStreakActive,
            isStreakActive = active,
            isPreviousStreakPreserved = wasStreakActive && !previousStreakBroken,
        )
    }
}

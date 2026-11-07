package com.jaroapps.dailybrain.common.domain.model

import java.time.LocalDate

data class StreakPatchProcessingResult(
    val newlyPatchedDates: List<LocalDate>,
    val lastProcessedDate: LocalDate,
    val isStreakActive: Boolean,
)

object StreakPatchRules {
    /**
     * Processes dates in chronological order. A missed date ends the current run when no
     * patch is available; later dates stay unprotected until a real workout starts a new run.
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
        val newlyPatched = mutableListOf<LocalDate>()

        while (cursor.isBefore(throughDate)) {
            cursor = cursor.plusDays(1)
            when {
                cursor in trainingDates -> active = true
                cursor in existingPatchedDates -> active = true
                active && remaining > 0 -> {
                    newlyPatched += cursor
                    remaining--
                }
                else -> active = false
            }
        }

        return StreakPatchProcessingResult(
            newlyPatchedDates = newlyPatched,
            lastProcessedDate = cursor,
            isStreakActive = active,
        )
    }
}

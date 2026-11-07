package com.jaroapps.dailybrain.common.domain.repository

import com.jaroapps.dailybrain.common.domain.model.StreakPatchProcessingSummary
import kotlinx.coroutines.flow.Flow

interface StreakPatchRepository {
    /** Evaluates every not-yet-processed day up to yesterday and returns a processing summary. */
    suspend fun processMissedDays(): StreakPatchProcessingSummary

    /** Local yyyyMMdd date keys covered by patches for the selected profile. */
    fun observePatchedDates(): Flow<Set<Int>>
}

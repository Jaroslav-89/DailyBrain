package com.jaroapps.dailybrain.common.domain.repository

import kotlinx.coroutines.flow.Flow

interface StreakPatchRepository {
    /** Evaluates every not-yet-processed day up to yesterday and returns the number of patches used. */
    suspend fun processMissedDays(): Int

    /** Local yyyyMMdd date keys covered by patches for the selected profile. */
    fun observePatchedDates(): Flow<Set<Int>>
}

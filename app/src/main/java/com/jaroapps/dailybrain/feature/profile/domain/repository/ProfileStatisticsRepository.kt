package com.jaroapps.dailybrain.feature.profile.domain.repository

import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

data class BestExerciseResult(
    val exerciseType: String,
    val bestDurationMs: Long?,
    val bestCorrectCount: Int?,
)

data class ProfileStatistics(
    val totalExerciseCount: Int = 0,
    val lastSevenDaysExerciseCount: Int = 0,
    val totalDurationMs: Long = 0L,
    val currentStreakDays: Int = 0,
    val bestStreakDays: Int = 0,
    val firstTrainingDate: LocalDate? = null,
    val bestExerciseStats: List<BestExerciseResult> = emptyList(),
)

interface ProfileStatisticsRepository {
    fun observeProfileStatistics(): Flow<ProfileStatistics>
}

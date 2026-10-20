package com.jaroapps.dailybrain.feature.profile.domain.repository

import com.jaroapps.dailybrain.core.database.entity.ExerciseStatisticsEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

data class ProfileStatistics(
    val totalExerciseCount: Int = 0,
    val lastSevenDaysExerciseCount: Int = 0,
    val totalDurationMs: Long = 0L,
    val currentStreakDays: Int = 0,
    val bestStreakDays: Int = 0,
    val firstTrainingDate: LocalDate? = null,
    val bestExerciseStats: List<ExerciseStatisticsEntity> = emptyList(),
)

interface ProfileStatisticsRepository {
    fun observeProfileStatistics(): Flow<ProfileStatistics>
}

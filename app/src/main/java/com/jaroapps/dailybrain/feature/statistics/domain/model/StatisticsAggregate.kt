package com.jaroapps.dailybrain.feature.statistics.domain.model

import kotlinx.coroutines.flow.Flow

data class StatisticsAggregate(
    val overall: OverallStatistics?,
    val exerciseStatistics: List<ExerciseStatistics>,
    val dailyResults: List<DailyExerciseResult>,
)

data class OverallStatistics(
    val currentStreakDays: Int,
    val bestStreakDays: Int,
)

data class ExerciseStatistics(
    val exerciseType: String,
    val bestDurationMs: Long?,
    val bestAccuracyPercent: Double?,
    val bestCorrectCount: Int?,
)

data class DailyExerciseResult(
    val date: Int,
    val arithmeticBestTimeMs: Long?,
    val memoryBestCorrectCount: Int?,
    val stroopBestTimeMs: Long?,
    val countingBestTimeMs: Long?,
)

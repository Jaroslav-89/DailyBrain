package com.jaroapps.dailybrain.feature.statistics.presentation

import com.jaroapps.dailybrain.core.database.entity.DailyExerciseResultEntity

data class ExerciseStatistics(
    val trainingCount: Int = 0,
    val weeklyTrainingCount: Int = 0,
    val totalDurationMs: Long = 0L,
    val averageDurationMs: Long = 0L,
    val averageAccuracyPercent: Double = 0.0,
    val bestAccuracyPercent: Double = 0.0,
    val totalTaskCount: Int = 0,
    val totalCorrectCount: Int = 0,
)

data class StatisticsUiState(
    val overall: ExerciseStatistics = ExerciseStatistics(),
    val arithmetic: ExerciseStatistics = ExerciseStatistics(),
    val memoryWords: ExerciseStatistics = ExerciseStatistics(),
    val countingTraining: ExerciseStatistics = ExerciseStatistics(),
    val stroopTest: ExerciseStatistics = ExerciseStatistics(),
    val currentStreakDays: Int = 0,
    val bestStreakDays: Int = 0,
    val arithmeticBestDurationMs: Long? = null,
    val memoryBestCorrectCount: Int? = null,
    val memoryBestDurationMs: Long? = null,
    val countingBestDurationMs: Long? = null,
    val stroopBestDurationMs: Long? = null,
    val dailyResults: List<DailyExerciseResultEntity> = emptyList(),
)


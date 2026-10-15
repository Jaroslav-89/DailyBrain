package com.jaroapps.dailybrain.feature.statistics.presentation

data class StatisticsUiState(
    val totalTrainingCount: Int = 0,
    val weeklyTrainingCount: Int = 0,
    val averageAccuracyPercent: Double = 0.0,
    val averageDurationMs: Long = 0L,
    val bestAccuracyPercent: Double = 0.0,
)

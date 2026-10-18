package com.jaroapps.dailybrain.feature.counting.domain.model

data class CountingStatistics(
    val trainingCount: Int = 0,
    val totalDurationMs: Long = 0L,
    val bestDurationMs: Long? = null,
)
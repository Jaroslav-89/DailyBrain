package com.jaroapps.dailybrain.feature.history.domain.model

data class TrainingSession(
    val id: Long = 0,
    val exerciseType: String,
    val startedAtEpochMs: Long,
    val finishedAtEpochMs: Long,
    val durationMs: Long,
    val accuracyPercent: Double = 0.0,
    val taskCount: Int = 0,
    val correctCount: Int = 0,
)
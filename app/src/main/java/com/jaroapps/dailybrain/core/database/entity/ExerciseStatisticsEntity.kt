package com.jaroapps.dailybrain.core.database.entity

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "exercise_statistics",
    primaryKeys = ["userId", "exerciseType"],
    indices = [Index(value = ["userId", "exerciseType"])],
)
data class ExerciseStatisticsEntity(
    val userId: String,
    val exerciseType: String,
    val trainingCount: Int,
    val totalDurationMs: Long,
    val bestDurationMs: Long?,
    val bestAccuracyPercent: Double?,
    val bestCorrectCount: Int?,
    val lastTrainingAtEpochMs: Long,
)
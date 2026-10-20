package com.jaroapps.dailybrain.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exercise_statistics")
data class ExerciseStatisticsEntity(
    @PrimaryKey val exerciseType: String,
    val trainingCount: Int,
    val totalDurationMs: Long,
    val bestDurationMs: Long?,
    val bestAccuracyPercent: Double?,
    val bestCorrectCount: Int?,
    val lastTrainingAtEpochMs: Long,
)

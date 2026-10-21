package com.jaroapps.dailybrain.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "overall_statistics")
data class OverallStatisticsEntity(
    @PrimaryKey val userId: String,
    val totalTrainingCount: Int,
    val totalDurationMs: Long,
    val currentStreakDays: Int,
    val bestStreakDays: Int,
    val lastTrainingDate: Int?,
    val updatedAtEpochMs: Long,
)
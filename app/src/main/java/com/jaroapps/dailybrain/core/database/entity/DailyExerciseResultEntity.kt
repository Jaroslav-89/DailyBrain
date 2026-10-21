package com.jaroapps.dailybrain.core.database.entity

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "daily_exercise_results",
    primaryKeys = ["userId", "date"],
    indices = [Index(value = ["userId", "date"])],
)
data class DailyExerciseResultEntity(
    val userId: String,
    val date: Int,
    val arithmeticBestTimeMs: Long?,
    val memoryBestCorrectCount: Int?,
    val stroopBestTimeMs: Long?,
    val countingBestTimeMs: Long?,
)
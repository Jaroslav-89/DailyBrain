package com.jaroapps.dailybrain.core.database.entity

import androidx.room.Entity
import androidx.room.Index

/**
 * Best result for each exercise on a calendar day and for a specific local account.
 */
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
package com.jaroapps.dailybrain.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Best result for each exercise on a calendar day.
 * A null field means that the exercise has no eligible result for that day.
 */
@Entity(tableName = "daily_exercise_results")
data class DailyExerciseResultEntity(
    @PrimaryKey val date: Int,
    val arithmeticBestTimeMs: Long?,
    val memoryBestCorrectCount: Int?,
    val stroopBestTimeMs: Long?,
    val countingBestTimeMs: Long?,
)

package com.jaroapps.dailybrain.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_exercise_results")
data class DailyExerciseResultEntity(
    @PrimaryKey val date: Int,
    val arithmeticBestTimeMs: Long? = null,
    val memoryBestCorrectCount: Int? = null,
    val stroopBestTimeMs: Long? = null,
    val countingBestTimeMs: Long? = null,
)

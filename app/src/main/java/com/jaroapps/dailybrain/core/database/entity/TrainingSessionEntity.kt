package com.jaroapps.dailybrain.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "training_sessions",
    indices = [
        Index(value = ["userId", "finishedAtEpochMs"]),
        Index(value = ["userId", "date"]),
    ],
)
data class TrainingSessionEntity(
    @PrimaryKey(autoGenerate = false)
    val id: Long,
    val userId: String,
    val exerciseType: String,
    val startedAtEpochMs: Long,
    val finishedAtEpochMs: Long,
    val date: Int,
    val durationMs: Long,
    val accuracyPercent: Double,
    val taskCount: Int,
    val correctCount: Int,
)
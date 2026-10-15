package com.jaroapps.dailybrain.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "training_sessions")
data class TrainingSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val exerciseType: String,
    val startedAtEpochMs: Long,
    val finishedAtEpochMs: Long,
    val date: Int,
    val durationMs: Long,
    val accuracyPercent: Double,
    val taskCount: Int,
    val correctCount: Int,
)
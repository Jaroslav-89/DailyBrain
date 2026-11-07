package com.jaroapps.dailybrain.core.database.entity

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "streak_patch_days",
    primaryKeys = ["userId", "date"],
    indices = [Index(value = ["userId", "date"])],
)
data class StreakPatchDayEntity(
    val userId: String,
    /** Local calendar date in yyyyMMdd format. */
    val date: Int,
    val appliedAtEpochMs: Long,
)

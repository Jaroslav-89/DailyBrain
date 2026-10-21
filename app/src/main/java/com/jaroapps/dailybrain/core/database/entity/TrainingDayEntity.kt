package com.jaroapps.dailybrain.core.database.entity

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "training_days",
    primaryKeys = ["userId", "date"],
    indices = [Index(value = ["userId", "date"])],
)
data class TrainingDayEntity(
    val userId: String,
    val date: Int,
    val exerciseCount: Int,
)
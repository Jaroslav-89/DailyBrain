package com.jaroapps.dailybrain.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "training_days")
data class TrainingDayEntity(
    @PrimaryKey
    val date: Int,
    val exerciseCount: Int,
)
package com.jaroapps.dailybrain.core.database.entity

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "user_inventory",
    primaryKeys = ["ownerUserId", "itemId"],
    indices = [Index(value = ["ownerUserId", "itemType"])],
)
data class UserInventoryEntity(
    val ownerUserId: String,
    val itemId: String,
    val itemType: String,
    val acquiredAtEpochMs: Long,
)

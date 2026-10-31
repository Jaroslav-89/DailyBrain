package com.jaroapps.dailybrain.core.database.entity

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "user_gifts",
    primaryKeys = ["ownerUserId", "giftId"],
    indices = [Index(value = ["ownerUserId", "receivedAtEpochMs"])],
)
data class UserGiftEntity(
    val ownerUserId: String,
    val giftId: String,
    val giftType: String,
    val senderUserId: String? = null,
    val receivedAtEpochMs: Long,
    val isOpened: Boolean = false,
)

package com.jaroapps.dailybrain.core.database.entity

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "gift_outbox",
    primaryKeys = ["ownerUserId", "giftId"],
    indices = [Index(value = ["ownerUserId", "isSent", "createdAtEpochMs"])],
)
data class GiftOutboxEntity(
    val ownerUserId: String,
    val giftId: String,
    val senderFirebaseUid: String,
    val recipientFirebaseUid: String,
    val senderName: String,
    val giftType: String,
    val amount: Int,
    val createdAtEpochMs: Long,
    val isSent: Boolean = false,
)

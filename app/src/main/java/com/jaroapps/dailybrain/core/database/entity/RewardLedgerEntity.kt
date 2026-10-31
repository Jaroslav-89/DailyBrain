package com.jaroapps.dailybrain.core.database.entity

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "reward_ledger",
    primaryKeys = ["userId", "rewardKey"],
    indices = [
        Index(value = ["userId", "category"]),
        Index(value = ["userId", "periodKey"]),
    ],
)
data class RewardLedgerEntity(
    val userId: String,
    val rewardKey: String,
    val deltaXp: Int,
    val deltaEndorphins: Int,
    val category: String,
    val reason: String,
    val sourceId: Long? = null,
    val periodKey: String? = null,
    val occurredAtEpochMs: Long,
    val isActive: Boolean = true,
)

package com.jaroapps.dailybrain.core.database.entity

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "user_badges",
    primaryKeys = ["ownerUserId", "badgeId"],
    indices = [
        Index(value = ["ownerUserId", "badgeType"]),
        Index(value = ["ownerUserId", "periodKey"]),
    ],
)
data class UserBadgeEntity(
    val ownerUserId: String,
    val badgeId: String,
    val badgeType: String,
    val periodKey: String,
    val awardedAtEpochMs: Long,
    val triggerWorkoutId: Long?,
    val isRevoked: Boolean = false,
)

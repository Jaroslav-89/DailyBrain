package com.jaroapps.dailybrain.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "pending_profile_deletions",
    indices = [Index(value = ["googleSubject"])],
)
data class PendingProfileDeletionEntity(
    @PrimaryKey val firebaseUid: String,
    val googleSubject: String?,
    val status: String,
    val requestedAtEpochMs: Long,
)

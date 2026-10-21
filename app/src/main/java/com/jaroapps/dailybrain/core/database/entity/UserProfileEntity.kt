package com.jaroapps.dailybrain.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "user_profile",
    indices = [Index(value = ["localAccountId"], unique = true)],
)
data class UserProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val localAccountId: String,
    val firebaseUid: String? = null,
    val displayName: String? = null,
    val generatedAlias: String,
    val gender: String? = null,
    /** ISO-8601 calendar date (yyyy-MM-dd), not an epoch timestamp. */
    val birthDate: String? = null,
    val country: String? = null,
    val city: String? = null,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
    /** Versioned, local-only avatar configuration JSON. */
    val avatarJson: String? = null,
)
package com.jaroapps.dailybrain.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
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
) {
    companion object {
        const val SINGLETON_ID = 1
    }
}

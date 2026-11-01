package com.jaroapps.dailybrain.core.database.entity

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "feed_events",
    primaryKeys = ["localAccountId", "eventId"],
    indices = [
        Index(value = ["localAccountId", "occurredAtEpochMs"]),
        Index(value = ["authorFirebaseUid"]),
    ],
)
data class FeedEventEntity(
    val localAccountId: String,
    val eventId: String,
    val authorFirebaseUid: String?,
    val authorName: String,
    val authorUsername: String?,
    val eventType: String,
    val occurredAtEpochMs: Long,
    val periodKey: String? = null,
    val exerciseType: String? = null,
    val valueLong: Long? = null,
    val valueInt: Int? = null,
    val accuracyPercent: Double? = null,
    val relatedUserFirebaseUid: String? = null,
    val relatedUserName: String? = null,
    val isLocal: Boolean,
    val isRevoked: Boolean = false,
    /** LOCAL_ONLY, PENDING_UPSERT, PENDING_DELETE, or SYNCED. */
    val syncState: String = "LOCAL_ONLY",
)

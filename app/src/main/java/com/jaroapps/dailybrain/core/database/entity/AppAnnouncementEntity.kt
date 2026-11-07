package com.jaroapps.dailybrain.core.database.entity

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "app_announcements",
    primaryKeys = ["userId", "announcementId"],
    indices = [Index(value = ["userId", "isDismissed", "createdAtEpochMs"])],
)
data class AppAnnouncementEntity(
    val userId: String,
    val announcementId: String,
    val type: String,
    val payload: String,
    val createdAtEpochMs: Long,
    val isDismissed: Boolean = false,
)

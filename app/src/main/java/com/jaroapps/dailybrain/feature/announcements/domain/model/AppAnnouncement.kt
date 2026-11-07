package com.jaroapps.dailybrain.feature.announcements.domain.model

data class AppAnnouncement(
    val id: String,
    val type: String,
    val payload: String,
    val createdAtEpochMs: Long,
)

object AppAnnouncementType {
    const val MISSED_STREAK = "MISSED_STREAK"
    const val ACHIEVEMENTS = "ACHIEVEMENTS"
    const val COLLECTIBLE = "COLLECTIBLE"
}

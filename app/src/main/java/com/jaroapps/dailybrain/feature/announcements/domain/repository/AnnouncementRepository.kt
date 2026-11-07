package com.jaroapps.dailybrain.feature.announcements.domain.repository

import com.jaroapps.dailybrain.feature.announcements.domain.model.AppAnnouncement
import kotlinx.coroutines.flow.Flow

interface AnnouncementRepository {
    fun observeNext(): Flow<AppAnnouncement?>

    suspend fun enqueue(
        announcementId: String,
        type: String,
        payload: String,
        createdAtEpochMs: Long = System.currentTimeMillis(),
    )

    suspend fun dismiss(announcementId: String)
}

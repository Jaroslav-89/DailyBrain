package com.jaroapps.dailybrain.feature.announcements.data

import com.jaroapps.dailybrain.core.database.ActiveUserProvider
import com.jaroapps.dailybrain.core.database.dao.AppAnnouncementDao
import com.jaroapps.dailybrain.core.database.entity.AppAnnouncementEntity
import com.jaroapps.dailybrain.feature.announcements.domain.model.AppAnnouncement
import com.jaroapps.dailybrain.feature.announcements.domain.repository.AnnouncementRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

class AnnouncementRepositoryImpl(
    private val dao: AppAnnouncementDao,
    private val activeUserProvider: ActiveUserProvider,
) : AnnouncementRepository {
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeNext(): Flow<AppAnnouncement?> =
        activeUserProvider.observeUserId().flatMapLatest { userId ->
            dao.observeNext(userId).map { entity ->
                entity?.let {
                    AppAnnouncement(it.announcementId, it.type, it.payload, it.createdAtEpochMs)
                }
            }
        }

    override suspend fun enqueue(
        announcementId: String,
        type: String,
        payload: String,
        createdAtEpochMs: Long,
    ) {
        require(announcementId.isNotBlank())
        require(type.isNotBlank())
        dao.insertIfAbsent(
            AppAnnouncementEntity(
                userId = activeUserProvider.currentUserId(),
                announcementId = announcementId,
                type = type,
                payload = payload,
                createdAtEpochMs = createdAtEpochMs,
            ),
        )
    }

    override suspend fun dismiss(announcementId: String) {
        dao.markDismissed(activeUserProvider.currentUserId(), announcementId)
    }
}

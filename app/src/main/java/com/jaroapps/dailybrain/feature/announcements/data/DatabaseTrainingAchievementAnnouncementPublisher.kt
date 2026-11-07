package com.jaroapps.dailybrain.feature.announcements.data

import com.jaroapps.dailybrain.common.domain.model.TrainingSession
import com.jaroapps.dailybrain.common.domain.service.TrainingAchievementAnnouncementPublisher
import com.jaroapps.dailybrain.core.database.ActiveUserProvider
import com.jaroapps.dailybrain.core.database.AppDatabase
import com.jaroapps.dailybrain.feature.announcements.domain.model.AppAnnouncementType
import com.jaroapps.dailybrain.feature.announcements.domain.repository.AnnouncementRepository
import org.json.JSONArray
import org.json.JSONObject

class DatabaseTrainingAchievementAnnouncementPublisher(
    private val database: AppDatabase,
    private val activeUserProvider: ActiveUserProvider,
    private val announcementRepository: AnnouncementRepository,
) : TrainingAchievementAnnouncementPublisher {

    override suspend fun onTrainingCompleted(session: TrainingSession) {
        publishEventsAt(
            timestamp = session.finishedAtEpochMs,
            announcementId = "training-achievements:" + session.startedAtEpochMs,
        )
    }

    override suspend fun onStreakPatchesApplied(appliedAtEpochMs: Long) {
        publishEventsAt(
            timestamp = appliedAtEpochMs,
            announcementId = "patch-achievements:" + appliedAtEpochMs,
        )
    }

    private suspend fun publishEventsAt(timestamp: Long, announcementId: String) {
        val userId = activeUserProvider.currentUserId()
        val events = database.feedEventDao().getLocalEvents(userId)
            .filter { !it.isRevoked && it.occurredAtEpochMs == timestamp && it.eventType in ANNOUNCEMENT_EVENT_TYPES }
            .distinctBy { it.eventId }
        if (events.isEmpty()) return
        val items = JSONArray()
        events.forEach { event ->
            items.put(
                JSONObject()
                    .put("eventType", event.eventType)
                    .put("valueInt", event.valueInt ?: JSONObject.NULL)
                    .put("valueLong", event.valueLong ?: JSONObject.NULL)
                    .put("exerciseType", event.exerciseType ?: JSONObject.NULL),
            )
        }
        announcementRepository.enqueue(
            announcementId = announcementId,
            type = AppAnnouncementType.ACHIEVEMENTS,
            payload = JSONObject().put("items", items).toString(),
            createdAtEpochMs = timestamp,
        )
    }

    private companion object {
        val ANNOUNCEMENT_EVENT_TYPES = setOf(
            "WEEKLY_XP_MILESTONE",
            "WEEKLY_STREAK",
            "WEEKLY_CHALLENGE",
            "MONTHLY_STREAK",
            "PERSONAL_RECORD",
        )
    }
}

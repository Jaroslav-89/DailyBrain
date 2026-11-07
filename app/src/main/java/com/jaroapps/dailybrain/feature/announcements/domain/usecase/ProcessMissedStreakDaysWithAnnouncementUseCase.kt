package com.jaroapps.dailybrain.feature.announcements.domain.usecase

import com.jaroapps.dailybrain.common.domain.service.TrainingAchievementAnnouncementPublisher
import com.jaroapps.dailybrain.common.domain.usecase.ProcessMissedStreakDaysUseCase
import com.jaroapps.dailybrain.feature.announcements.domain.model.AppAnnouncementType
import com.jaroapps.dailybrain.feature.announcements.domain.repository.AnnouncementRepository
import org.json.JSONObject

/**
 * Coordinates streak maintenance with its one-time user-facing explanation.
 * The underlying patch processor remains unaware of presentation and navigation.
 */
class ProcessMissedStreakDaysWithAnnouncementUseCase(
    private val processMissedStreakDays: ProcessMissedStreakDaysUseCase,
    private val announcementRepository: AnnouncementRepository,
    private val achievementAnnouncementPublisher: TrainingAchievementAnnouncementPublisher,
) {
    suspend operator fun invoke() = processMissedStreakDays().also { result ->
        if (result.missedDayCount > 0) {
            val lastDate = result.lastProcessedDate?.toString() ?: "unknown"
            announcementRepository.enqueue(
                announcementId = "missed-streak:" + lastDate,
                type = AppAnnouncementType.MISSED_STREAK,
                payload = JSONObject()
                    .put("missedDays", result.missedDayCount)
                    .put("usedPatches", result.usedPatchCount)
                    .put("remainingPatches", result.remainingPatchCount)
                    .put("hadActiveStreak", result.wasStreakActive)
                    .put("streakPreserved", result.isStreakPreserved)
                    .put("streakActiveAfter", result.isStreakActiveAfter)
                    .toString(),
                createdAtEpochMs = result.patchesAppliedAtEpochMs ?: System.currentTimeMillis(),
            )
        }
        result.patchesAppliedAtEpochMs?.let {
            achievementAnnouncementPublisher.onStreakPatchesApplied(it)
        }
    }
}

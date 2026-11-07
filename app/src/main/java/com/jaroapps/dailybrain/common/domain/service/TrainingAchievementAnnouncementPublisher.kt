package com.jaroapps.dailybrain.common.domain.service

import com.jaroapps.dailybrain.common.domain.model.TrainingSession

/**
 * Publishes presentation-only announcements for achievement events already recorded by the
 * reward recalculator. Implementations must be idempotent for a given training or patch event.
 */
interface TrainingAchievementAnnouncementPublisher {
    suspend fun onTrainingCompleted(session: TrainingSession)
    suspend fun onStreakPatchesApplied(appliedAtEpochMs: Long)
}

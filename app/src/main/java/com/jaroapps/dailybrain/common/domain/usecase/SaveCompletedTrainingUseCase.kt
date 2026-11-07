package com.jaroapps.dailybrain.common.domain.usecase

import com.jaroapps.dailybrain.common.domain.model.TrainingSession
import com.jaroapps.dailybrain.common.domain.repository.CompletedTrainingRepository
import com.jaroapps.dailybrain.common.domain.service.FeedSyncTrigger
import com.jaroapps.dailybrain.common.domain.service.TrainingAchievementAnnouncementPublisher
import com.jaroapps.dailybrain.common.domain.service.TrainingStateChangedNotifier
import kotlinx.coroutines.CancellationException

class SaveCompletedTrainingUseCase(
    private val repository: CompletedTrainingRepository,
    private val stateChangedNotifier: TrainingStateChangedNotifier = TrainingStateChangedNotifier {},
    private val feedSyncTrigger: FeedSyncTrigger? = null,
    private val trainingAchievementPublisher: TrainingAchievementAnnouncementPublisher? = null,
) {
    suspend operator fun invoke(session: TrainingSession) {
        repository.saveCompletedTraining(session)
        try {
            trainingAchievementPublisher?.onTrainingCompleted(session)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            // The completed workout must remain successful even if an informational event
            // cannot be queued. Reward and training data have already been saved.
        }
        feedSyncTrigger?.requestSync()
        stateChangedNotifier.onTrainingStateChanged()
    }
}

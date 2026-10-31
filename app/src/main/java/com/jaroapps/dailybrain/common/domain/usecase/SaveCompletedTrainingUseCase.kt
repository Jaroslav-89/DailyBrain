package com.jaroapps.dailybrain.common.domain.usecase

import com.jaroapps.dailybrain.common.domain.model.TrainingSession
import com.jaroapps.dailybrain.common.domain.repository.CompletedTrainingRepository
import com.jaroapps.dailybrain.common.domain.service.TrainingStateChangedNotifier
import com.jaroapps.dailybrain.common.domain.service.FeedSyncTrigger

class SaveCompletedTrainingUseCase(
    private val repository: CompletedTrainingRepository,
    private val stateChangedNotifier: TrainingStateChangedNotifier = TrainingStateChangedNotifier {},
    private val feedSyncTrigger: FeedSyncTrigger? = null,
) {
    suspend operator fun invoke(session: TrainingSession) {
        repository.saveCompletedTraining(session)
        feedSyncTrigger?.requestSync()
        stateChangedNotifier.onTrainingStateChanged()
    }
}

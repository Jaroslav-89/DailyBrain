package com.jaroapps.dailybrain.common.domain.usecase

import com.jaroapps.dailybrain.common.domain.repository.CompletedTrainingRepository
import com.jaroapps.dailybrain.common.domain.service.TrainingStateChangedNotifier

/**
 * Deletes a completed training and rebuilds statistics derived from training history.
 */
class DeleteCompletedTrainingUseCase(
    private val repository: CompletedTrainingRepository,
    private val stateChangedNotifier: TrainingStateChangedNotifier = TrainingStateChangedNotifier {},
) {
    suspend operator fun invoke(trainingId: Long): Boolean {
        val deleted = repository.deleteCompletedTraining(trainingId)
        if (deleted) stateChangedNotifier.onTrainingStateChanged()
        return deleted
    }
}

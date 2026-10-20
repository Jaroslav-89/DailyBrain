package com.jaroapps.dailybrain.common.domain.usecase

import com.jaroapps.dailybrain.common.domain.repository.CompletedTrainingRepository

/**
 * Deletes a completed training and rebuilds statistics derived from training history.
 */
class DeleteCompletedTrainingUseCase(
    private val repository: CompletedTrainingRepository,
) {
    suspend operator fun invoke(trainingId: Long): Boolean =
        repository.deleteCompletedTraining(trainingId)
}

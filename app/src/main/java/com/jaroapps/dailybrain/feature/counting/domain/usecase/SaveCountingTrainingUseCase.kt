package com.jaroapps.dailybrain.feature.counting.domain.usecase

import com.jaroapps.dailybrain.common.domain.model.TrainingSession
import com.jaroapps.dailybrain.common.domain.repository.CompletedTrainingRepository

class SaveCountingTrainingUseCase(
    private val completedTrainingRepository: CompletedTrainingRepository,
) {
    suspend operator fun invoke(session: TrainingSession) {
        completedTrainingRepository.saveCompletedTraining(session)
    }
}

package com.jaroapps.dailybrain.feature.arithmetic.domain.usecase

import com.jaroapps.dailybrain.common.domain.model.TrainingSession
import com.jaroapps.dailybrain.common.domain.repository.CompletedTrainingRepository

class SaveArithmeticTrainingUseCase(
    private val completedTrainingRepository: CompletedTrainingRepository,
) {
    suspend operator fun invoke(session: TrainingSession) {
        completedTrainingRepository.saveCompletedTraining(session)
    }
}

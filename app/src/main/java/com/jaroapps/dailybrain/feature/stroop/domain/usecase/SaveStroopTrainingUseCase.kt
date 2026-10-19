package com.jaroapps.dailybrain.feature.stroop.domain.usecase

import com.jaroapps.dailybrain.common.domain.model.TrainingSession
import com.jaroapps.dailybrain.common.domain.repository.CompletedTrainingRepository

class SaveStroopTrainingUseCase(
    private val completedTrainingRepository: CompletedTrainingRepository,
) {
    suspend operator fun invoke(session: TrainingSession) {
        completedTrainingRepository.saveCompletedTraining(session)
    }
}

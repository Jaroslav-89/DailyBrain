package com.jaroapps.dailybrain.common.domain.usecase

import com.jaroapps.dailybrain.common.domain.model.TrainingSession
import com.jaroapps.dailybrain.common.domain.repository.CompletedTrainingRepository

class SaveCompletedTrainingUseCase(
    private val repository: CompletedTrainingRepository,
) {
    suspend operator fun invoke(session: TrainingSession) {
        repository.saveCompletedTraining(session)
    }
}

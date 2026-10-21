package com.jaroapps.dailybrain.common.domain.usecase

import com.jaroapps.dailybrain.common.domain.model.TrainingSession
import com.jaroapps.dailybrain.common.domain.repository.CompletedTrainingRepository
import com.jaroapps.dailybrain.common.domain.service.TrainingStateChangedNotifier

class SaveCompletedTrainingUseCase(
    private val repository: CompletedTrainingRepository,
    private val stateChangedNotifier: TrainingStateChangedNotifier = TrainingStateChangedNotifier {},
) {
    suspend operator fun invoke(session: TrainingSession) {
        repository.saveCompletedTraining(session)
        stateChangedNotifier.onTrainingStateChanged()
    }
}

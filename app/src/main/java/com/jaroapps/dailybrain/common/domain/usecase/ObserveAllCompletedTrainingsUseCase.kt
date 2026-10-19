package com.jaroapps.dailybrain.common.domain.usecase

import com.jaroapps.dailybrain.common.domain.model.TrainingSession
import com.jaroapps.dailybrain.common.domain.repository.CompletedTrainingRepository
import kotlinx.coroutines.flow.Flow

class ObserveAllCompletedTrainingsUseCase(
    private val repository: CompletedTrainingRepository,
) {
    operator fun invoke(): Flow<List<TrainingSession>> =
        repository.observeAllCompletedTrainings()
}

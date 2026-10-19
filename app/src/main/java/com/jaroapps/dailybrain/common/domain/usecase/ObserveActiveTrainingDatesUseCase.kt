package com.jaroapps.dailybrain.common.domain.usecase

import com.jaroapps.dailybrain.common.domain.repository.CompletedTrainingRepository
import kotlinx.coroutines.flow.Flow

class ObserveActiveTrainingDatesUseCase(
    private val repository: CompletedTrainingRepository,
) {
    operator fun invoke(): Flow<Set<Int>> = repository.observeActiveTrainingDates()
}

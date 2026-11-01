package com.jaroapps.dailybrain.common.domain.usecase

import com.jaroapps.dailybrain.common.domain.repository.CompletedTrainingRepository
import kotlinx.coroutines.flow.Flow

class ObserveTotalTrainingCountUseCase(
    private val repository: CompletedTrainingRepository,
) {
    operator fun invoke(): Flow<Int> = repository.observeTotalTrainingCount()
}

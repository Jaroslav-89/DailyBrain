package com.jaroapps.dailybrain.common.domain.usecase

import com.jaroapps.dailybrain.common.domain.repository.CompletedTrainingRepository

class InitializeRewardStateUseCase(
    private val repository: CompletedTrainingRepository,
) {
    suspend operator fun invoke() = repository.initializeRewardState()
}

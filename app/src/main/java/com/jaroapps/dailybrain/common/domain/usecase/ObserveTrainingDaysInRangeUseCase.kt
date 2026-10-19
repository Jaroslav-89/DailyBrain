package com.jaroapps.dailybrain.common.domain.usecase

import com.jaroapps.dailybrain.common.domain.repository.CompletedTrainingRepository
import kotlinx.coroutines.flow.Flow

class ObserveTrainingDaysInRangeUseCase(
    private val repository: CompletedTrainingRepository,
) {
    operator fun invoke(startDate: Int, endDate: Int): Flow<Set<Int>> =
        repository.observeTrainingDaysBetween(startDate, endDate)
}

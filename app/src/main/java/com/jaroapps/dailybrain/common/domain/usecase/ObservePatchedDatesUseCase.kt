package com.jaroapps.dailybrain.common.domain.usecase

import com.jaroapps.dailybrain.common.domain.repository.StreakPatchRepository
import kotlinx.coroutines.flow.Flow

class ObservePatchedDatesUseCase(
    private val repository: StreakPatchRepository,
) {
    operator fun invoke(): Flow<Set<Int>> = repository.observePatchedDates()
}

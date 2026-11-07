package com.jaroapps.dailybrain.common.domain.usecase

import com.jaroapps.dailybrain.common.domain.repository.StreakPatchRepository

class ProcessMissedStreakDaysUseCase(
    private val repository: StreakPatchRepository,
) {
    suspend operator fun invoke(): Int = repository.processMissedDays()
}

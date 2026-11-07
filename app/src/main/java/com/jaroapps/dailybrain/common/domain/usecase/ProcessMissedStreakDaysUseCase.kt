package com.jaroapps.dailybrain.common.domain.usecase

import com.jaroapps.dailybrain.common.domain.model.StreakPatchProcessingSummary
import com.jaroapps.dailybrain.common.domain.repository.StreakPatchRepository

class ProcessMissedStreakDaysUseCase(
    private val repository: StreakPatchRepository,
) {
    suspend operator fun invoke(): StreakPatchProcessingSummary = repository.processMissedDays()
}

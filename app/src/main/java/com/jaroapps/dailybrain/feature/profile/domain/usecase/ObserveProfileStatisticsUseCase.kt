package com.jaroapps.dailybrain.feature.profile.domain.usecase

import com.jaroapps.dailybrain.feature.profile.domain.repository.ProfileStatistics
import com.jaroapps.dailybrain.feature.profile.domain.repository.ProfileStatisticsRepository
import kotlinx.coroutines.flow.Flow

class ObserveProfileStatisticsUseCase(
    private val repository: ProfileStatisticsRepository,
) {
    operator fun invoke(): Flow<ProfileStatistics> = repository.observeProfileStatistics()
}

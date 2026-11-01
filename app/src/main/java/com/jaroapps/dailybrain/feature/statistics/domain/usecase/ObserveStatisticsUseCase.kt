package com.jaroapps.dailybrain.feature.statistics.domain.usecase

import com.jaroapps.dailybrain.feature.statistics.domain.model.StatisticsAggregate
import com.jaroapps.dailybrain.feature.statistics.domain.repository.StatisticsRepository
import kotlinx.coroutines.flow.Flow

class ObserveStatisticsUseCase(
    private val repository: StatisticsRepository,
) {
    operator fun invoke(): Flow<StatisticsAggregate> = repository.observeStatistics()
}

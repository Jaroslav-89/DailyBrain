package com.jaroapps.dailybrain.feature.statistics.domain.repository

import com.jaroapps.dailybrain.feature.statistics.domain.model.StatisticsAggregate
import kotlinx.coroutines.flow.Flow

interface StatisticsRepository {
    fun observeStatistics(): Flow<StatisticsAggregate>
}

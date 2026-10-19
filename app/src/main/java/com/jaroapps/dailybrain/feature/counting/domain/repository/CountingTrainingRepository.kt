package com.jaroapps.dailybrain.feature.counting.domain.repository

import com.jaroapps.dailybrain.feature.counting.domain.model.CountingStatistics
import kotlinx.coroutines.flow.Flow

interface CountingTrainingRepository {
    fun observeStatistics(): Flow<CountingStatistics>
}

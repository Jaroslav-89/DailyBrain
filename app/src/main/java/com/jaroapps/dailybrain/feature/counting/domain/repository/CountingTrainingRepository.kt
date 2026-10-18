package com.jaroapps.dailybrain.feature.counting.domain.repository

import com.jaroapps.dailybrain.common.domain.model.TrainingSession
import com.jaroapps.dailybrain.feature.counting.domain.model.CountingStatistics
import kotlinx.coroutines.flow.Flow

interface CountingTrainingRepository {

    suspend fun saveCompletedTraining(
        session: TrainingSession,
    )

    fun observeStatistics(): Flow<CountingStatistics>
}
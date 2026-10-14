package com.jaroapps.dailybrain.feature.history.domain.repository

import com.jaroapps.dailybrain.feature.history.domain.model.TrainingSession
import kotlinx.coroutines.flow.Flow

interface TrainingSessionRepository {

    suspend fun save(session: TrainingSession)

    fun observeAll(): Flow<List<TrainingSession>>
}
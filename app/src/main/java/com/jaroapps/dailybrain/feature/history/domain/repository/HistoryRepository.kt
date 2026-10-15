package com.jaroapps.dailybrain.feature.history.domain.repository

import com.jaroapps.dailybrain.common.domain.model.TrainingSession
import kotlinx.coroutines.flow.Flow

interface HistoryRepository {
    fun observeAll(): Flow<List<TrainingSession>>
    fun observeByDate(date: Int): Flow<List<TrainingSession>>
}
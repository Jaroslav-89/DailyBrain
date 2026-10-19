package com.jaroapps.dailybrain.common.domain.repository

import com.jaroapps.dailybrain.common.domain.model.TrainingSession
import kotlinx.coroutines.flow.Flow

/**
 * Общий доступ к завершённым тренировкам: сохранение и чтение истории.
 */
interface CompletedTrainingRepository {
    suspend fun saveCompletedTraining(session: TrainingSession)
    fun observeAllCompletedTrainings(): Flow<List<TrainingSession>>
    fun observeCompletedTrainingsByDate(date: Int): Flow<List<TrainingSession>>
}

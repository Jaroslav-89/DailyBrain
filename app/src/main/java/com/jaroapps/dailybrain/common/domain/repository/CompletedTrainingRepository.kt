package com.jaroapps.dailybrain.common.domain.repository

import com.jaroapps.dailybrain.common.domain.model.TrainingSession
import kotlinx.coroutines.flow.Flow

/**
 * Общий контракт для сохранения завершённых тренировок и чтения истории.
 */
interface CompletedTrainingRepository {
    suspend fun saveCompletedTraining(session: TrainingSession)
    suspend fun deleteCompletedTraining(id: Long): Boolean
    suspend fun recalculateDailyExerciseResult(date: Int)
    fun observeAllCompletedTrainings(): Flow<List<TrainingSession>>
    fun observeCompletedTrainingsByDate(date: Int): Flow<List<TrainingSession>>
    fun observeActiveTrainingDates(): Flow<Set<Int>>
    fun observeTrainingDaysBetween(startDate: Int, endDate: Int): Flow<Set<Int>>
    fun observeTotalTrainingCount(): Flow<Int>
}

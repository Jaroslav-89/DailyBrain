package com.jaroapps.dailybrain.feature.history.data

import com.jaroapps.dailybrain.common.domain.model.TrainingSession
import com.jaroapps.dailybrain.core.database.dao.TrainingSessionDao
import com.jaroapps.dailybrain.feature.history.domain.repository.HistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class HistoryRepositoryImpl(
    private val dao: TrainingSessionDao,
) : HistoryRepository {

    override fun observeAll(): Flow<List<TrainingSession>> {
        return dao
            .observeAll()
            .map { entities ->
                entities.map { entity ->
                    TrainingSession(
                        id = entity.id,
                        exerciseType = entity.exerciseType,
                        startedAtEpochMs = entity.startedAtEpochMs,
                        finishedAtEpochMs = entity.finishedAtEpochMs,
                        durationMs = entity.durationMs,
                        accuracyPercent = entity.accuracyPercent,
                        taskCount = entity.taskCount,
                        correctCount = entity.correctCount,
                    )
                }
            }
    }


    override fun observeByDate(date: Int): Flow<List<TrainingSession>> {
        return dao
            .observeByDate(date)
            .map { entities ->
                entities.map { entity ->
                    TrainingSession(
                        id = entity.id,
                        exerciseType = entity.exerciseType,
                        startedAtEpochMs = entity.startedAtEpochMs,
                        finishedAtEpochMs = entity.finishedAtEpochMs,
                        durationMs = entity.durationMs,
                        accuracyPercent = entity.accuracyPercent,
                        taskCount = entity.taskCount,
                        correctCount = entity.correctCount,
                    )
                }
            }
    }
}
package com.jaroapps.dailybrain.feature.history.data

import com.jaroapps.dailybrain.core.database.dao.TrainingSessionDao
import com.jaroapps.dailybrain.core.database.entity.TrainingSessionEntity
import com.jaroapps.dailybrain.core.utils.formatMillisToDate
import com.jaroapps.dailybrain.feature.history.domain.model.TrainingSession
import com.jaroapps.dailybrain.feature.history.domain.repository.TrainingSessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TrainingSessionRepositoryImpl(
    private val dao: TrainingSessionDao,
) : TrainingSessionRepository {

    override suspend fun save(session: TrainingSession) {
        dao.insert(
            TrainingSessionEntity(
                exerciseType = session.exerciseType,
                startedAtEpochMs = session.startedAtEpochMs,
                finishedAtEpochMs = session.finishedAtEpochMs,
                date = formatMillisToDate(session.finishedAtEpochMs),
                durationMs = session.durationMs,
                accuracyPercent = session.accuracyPercent,
                taskCount = session.taskCount,
                correctCount = session.correctCount,
            )
        )
    }

    override fun observeAll(): Flow<List<TrainingSession>> {
        return dao.observeAll().map { entities ->
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
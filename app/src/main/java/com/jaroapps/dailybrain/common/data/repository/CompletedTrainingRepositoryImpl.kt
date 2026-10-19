package com.jaroapps.dailybrain.common.data.repository

import androidx.room.withTransaction
import com.jaroapps.dailybrain.common.domain.model.TrainingSession
import com.jaroapps.dailybrain.common.domain.repository.CompletedTrainingRepository
import com.jaroapps.dailybrain.core.database.AppDatabase
import com.jaroapps.dailybrain.core.database.entity.TrainingDayEntity
import com.jaroapps.dailybrain.core.database.entity.TrainingSessionEntity
import com.jaroapps.dailybrain.core.utils.formatMillisToDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CompletedTrainingRepositoryImpl(
    private val database: AppDatabase,
) : CompletedTrainingRepository {

    override suspend fun saveCompletedTraining(session: TrainingSession) {
        val date = formatMillisToDate(session.finishedAtEpochMs)

        database.withTransaction {
            database.trainingSessionDao().insert(
                TrainingSessionEntity(
                    exerciseType = session.exerciseType,
                    startedAtEpochMs = session.startedAtEpochMs,
                    finishedAtEpochMs = session.finishedAtEpochMs,
                    date = date,
                    durationMs = session.durationMs,
                    accuracyPercent = session.accuracyPercent,
                    taskCount = session.taskCount,
                    correctCount = session.correctCount,
                )
            )

            val insertedId = database.trainingDayDao().insertIfAbsent(
                TrainingDayEntity(date = date, exerciseCount = 1)
            )

            if (insertedId == -1L) {
                database.trainingDayDao().incrementExerciseCount(date)
            }
        }
    }

    override fun observeAllCompletedTrainings(): Flow<List<TrainingSession>> =
        database.trainingSessionDao().observeAll().map { sessions ->
            sessions.map { entity -> entity.toDomain() }
        }

    override fun observeCompletedTrainingsByDate(date: Int): Flow<List<TrainingSession>> =
        database.trainingSessionDao().observeByDate(date).map { sessions ->
            sessions.map { session -> session.toDomain() }
        }

    override fun observeActiveTrainingDates(): Flow<Set<Int>> =
        database.trainingDayDao().observeAll().map { days ->
            days.map { it.date }.toSet()
        }

    private fun TrainingSessionEntity.toDomain() = TrainingSession(
        id = id,
        exerciseType = exerciseType,
        startedAtEpochMs = startedAtEpochMs,
        finishedAtEpochMs = finishedAtEpochMs,
        durationMs = durationMs,
        accuracyPercent = accuracyPercent,
        taskCount = taskCount,
        correctCount = correctCount,
    )
}

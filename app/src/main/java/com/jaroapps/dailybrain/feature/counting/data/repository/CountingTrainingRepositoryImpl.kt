package com.jaroapps.dailybrain.feature.counting.data.repository

import androidx.room.withTransaction
import com.jaroapps.dailybrain.common.domain.model.ExerciseType
import com.jaroapps.dailybrain.common.domain.model.TrainingSession
import com.jaroapps.dailybrain.core.database.AppDatabase
import com.jaroapps.dailybrain.core.database.entity.TrainingDayEntity
import com.jaroapps.dailybrain.core.database.entity.TrainingSessionEntity
import com.jaroapps.dailybrain.core.utils.formatMillisToDate
import com.jaroapps.dailybrain.feature.counting.domain.model.CountingStatistics
import com.jaroapps.dailybrain.feature.counting.domain.repository.CountingTrainingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CountingTrainingRepositoryImpl(
    private val database: AppDatabase,
) : CountingTrainingRepository {

    override suspend fun saveCompletedTraining(
        session: TrainingSession,
    ) {
        val date = formatMillisToDate(session.finishedAtEpochMs)

        database.withTransaction {
            database.trainingSessionDao().insert(
                TrainingSessionEntity(
                    exerciseType = session.exerciseType,
                    startedAtEpochMs = session.startedAtEpochMs,
                    finishedAtEpochMs = session.finishedAtEpochMs,
                    date = date,
                    durationMs = session.durationMs,
                    accuracyPercent = 0.0,
                    taskCount = 0,
                    correctCount = 0,
                )
            )

            val insertedId = database.trainingDayDao().insertIfAbsent(
                TrainingDayEntity(
                    date = date,
                    exerciseCount = 1,
                )
            )

            if (insertedId == -1L) {
                database.trainingDayDao().incrementExerciseCount(date)
            }
        }
    }

    override fun observeStatistics(): Flow<CountingStatistics> {
        return database.trainingSessionDao()
            .observeAll()
            .map { entities ->
                val sessions = entities.filter {
                    it.exerciseType ==
                            ExerciseType.COUNTING_TRAINING.storageValue
                }

                CountingStatistics(
                    trainingCount = sessions.size,
                    totalDurationMs = sessions.sumOf {
                        it.durationMs.coerceAtLeast(0L)
                    },
                    bestDurationMs = sessions.minOfOrNull {
                        it.durationMs.coerceAtLeast(0L)
                    },
                )
            }
    }
}
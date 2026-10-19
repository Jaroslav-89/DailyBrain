package com.jaroapps.dailybrain.common.data.repository

import androidx.room.withTransaction
import com.jaroapps.dailybrain.common.domain.model.TrainingSession
import com.jaroapps.dailybrain.common.domain.repository.CompletedTrainingRepository
import com.jaroapps.dailybrain.core.database.AppDatabase
import com.jaroapps.dailybrain.core.database.entity.TrainingDayEntity
import com.jaroapps.dailybrain.core.database.entity.TrainingSessionEntity
import com.jaroapps.dailybrain.core.utils.formatMillisToDate

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
}

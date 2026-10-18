package com.jaroapps.dailybrain.feature.memory.data

import androidx.room.withTransaction
import com.jaroapps.dailybrain.common.domain.model.TrainingSession
import com.jaroapps.dailybrain.core.database.AppDatabase
import com.jaroapps.dailybrain.core.database.entity.TrainingDayEntity
import com.jaroapps.dailybrain.core.database.entity.TrainingSessionEntity
import com.jaroapps.dailybrain.core.utils.formatMillisToDate
import com.jaroapps.dailybrain.feature.memory.domain.repository.MemoryTrainingRepository

class MemoryTrainingRepositoryImpl(
    private val database: AppDatabase
) : MemoryTrainingRepository {

    override suspend fun saveCompletedTraining(
        session: TrainingSession
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
                    accuracyPercent = session.accuracyPercent,
                    taskCount = session.taskCount,
                    correctCount = session.correctCount
                )
            )

            val insertedId = database.trainingDayDao().insertIfAbsent(
                TrainingDayEntity(
                    date = date,
                    exerciseCount = 1
                )
            )

            if (insertedId == -1L) {
                database.trainingDayDao()
                    .incrementExerciseCount(date)
            }
        }
    }
}
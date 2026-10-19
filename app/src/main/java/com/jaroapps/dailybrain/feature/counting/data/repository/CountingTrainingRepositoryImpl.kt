package com.jaroapps.dailybrain.feature.counting.data.repository

import com.jaroapps.dailybrain.common.domain.model.ExerciseType
import com.jaroapps.dailybrain.core.database.AppDatabase
import com.jaroapps.dailybrain.feature.counting.domain.model.CountingStatistics
import com.jaroapps.dailybrain.feature.counting.domain.repository.CountingTrainingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CountingTrainingRepositoryImpl(
    private val database: AppDatabase,
) : CountingTrainingRepository {

    override fun observeStatistics(): Flow<CountingStatistics> {
        return database.trainingSessionDao()
            .observeAll()
            .map { entities ->
                val sessions = entities.filter {
                    it.exerciseType == ExerciseType.COUNTING_TRAINING.storageValue
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

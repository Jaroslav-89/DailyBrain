package com.jaroapps.dailybrain.feature.counting.domain.usecase

import com.jaroapps.dailybrain.common.domain.model.ExerciseType
import com.jaroapps.dailybrain.common.domain.repository.CompletedTrainingRepository
import com.jaroapps.dailybrain.feature.counting.domain.model.CountingStatistics
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ObserveCountingStatisticsUseCase(
    private val completedTrainingRepository: CompletedTrainingRepository,
) {
    operator fun invoke(): Flow<CountingStatistics> =
        completedTrainingRepository.observeAllCompletedTrainings().map { sessions ->
            val countingSessions = sessions.filter {
                it.exerciseType == ExerciseType.COUNTING_TRAINING.storageValue
            }
            CountingStatistics(
                trainingCount = countingSessions.size,
                totalDurationMs = countingSessions.sumOf { it.durationMs.coerceAtLeast(0L) },
                bestDurationMs = countingSessions.minOfOrNull { it.durationMs.coerceAtLeast(0L) },
            )
        }
}

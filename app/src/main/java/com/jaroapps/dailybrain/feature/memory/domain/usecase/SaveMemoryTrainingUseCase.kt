package com.jaroapps.dailybrain.feature.memory.domain.usecase

import com.jaroapps.dailybrain.common.domain.model.TrainingSession
import com.jaroapps.dailybrain.feature.memory.domain.repository.MemoryTrainingRepository

class SaveMemoryTrainingUseCase(
    private val repository: MemoryTrainingRepository
) {

    suspend operator fun invoke(
        startedAtEpochMs: Long,
        finishedAtEpochMs: Long,
        result: com.jaroapps.dailybrain.feature.memory.domain.model.MemoryTrainingResult
    ) {
        require(finishedAtEpochMs >= startedAtEpochMs) {
            "Finish time cannot be earlier than start time"
        }

        val session = TrainingSession(
            exerciseType = EXERCISE_TYPE,
            startedAtEpochMs = startedAtEpochMs,
            finishedAtEpochMs = finishedAtEpochMs,
            durationMs = finishedAtEpochMs - startedAtEpochMs,
            accuracyPercent = result.accuracyPercent.toDouble(),
            taskCount = result.shownWords.size,
            correctCount = result.correctCount
        )

        repository.saveCompletedTraining(session)
    }

    private companion object {
        const val EXERCISE_TYPE = "MEMORY_WORDS"
    }
}
package com.jaroapps.dailybrain.feature.memory.domain.usecase

import com.jaroapps.dailybrain.common.domain.model.ExerciseType
import com.jaroapps.dailybrain.common.domain.model.TrainingSession
import com.jaroapps.dailybrain.common.domain.repository.CompletedTrainingRepository
import com.jaroapps.dailybrain.common.domain.service.TrainingAchievementAnnouncementPublisher
import com.jaroapps.dailybrain.common.domain.service.TrainingStateChangedNotifier
import com.jaroapps.dailybrain.feature.memory.domain.model.MemoryTrainingResult
import kotlinx.coroutines.CancellationException

class SaveMemoryTrainingUseCase(
    private val completedTrainingRepository: CompletedTrainingRepository,
    private val stateChangedNotifier: TrainingStateChangedNotifier = TrainingStateChangedNotifier {},
    private val trainingAchievementPublisher: TrainingAchievementAnnouncementPublisher? = null,
) {
    suspend operator fun invoke(
        startedAtEpochMs: Long,
        finishedAtEpochMs: Long,
        durationMs: Long,
        result: MemoryTrainingResult,
    ) {
        require(durationMs >= 0L) { "Duration cannot be negative" }
        val session = TrainingSession(
            exerciseType = ExerciseType.MEMORY_WORDS.storageValue,
            startedAtEpochMs = startedAtEpochMs,
            finishedAtEpochMs = finishedAtEpochMs,
            durationMs = durationMs,
            accuracyPercent = result.accuracyPercent.toDouble(),
            taskCount = result.shownWords.size,
            correctCount = result.correctCount,
        )
        completedTrainingRepository.saveCompletedTraining(session)
        try {
            trainingAchievementPublisher?.onTrainingCompleted(session)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            // Do not turn a successfully saved memory workout into a failed result screen.
        }
        stateChangedNotifier.onTrainingStateChanged()
    }
}

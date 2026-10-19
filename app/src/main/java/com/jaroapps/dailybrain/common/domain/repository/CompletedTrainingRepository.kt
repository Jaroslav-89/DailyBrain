package com.jaroapps.dailybrain.common.domain.repository

import com.jaroapps.dailybrain.common.domain.model.TrainingSession

/**
 * Единственная точка сохранения завершённых тренировок всех типов.
 */
interface CompletedTrainingRepository {
    suspend fun saveCompletedTraining(session: TrainingSession)
}

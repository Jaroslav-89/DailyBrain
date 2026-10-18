package com.jaroapps.dailybrain.feature.memory.domain.repository

import com.jaroapps.dailybrain.common.domain.model.TrainingSession

interface MemoryTrainingRepository {

    suspend fun saveCompletedTraining(
        session: TrainingSession
    )
}
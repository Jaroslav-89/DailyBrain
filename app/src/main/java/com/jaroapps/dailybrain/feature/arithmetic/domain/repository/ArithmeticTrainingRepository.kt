package com.jaroapps.dailybrain.feature.arithmetic.domain.repository

import com.jaroapps.dailybrain.common.domain.model.TrainingSession

interface ArithmeticTrainingRepository {

    suspend fun saveCompletedTraining(
        session: TrainingSession,
    )
}

package com.jaroapps.dailybrain.feature.stroop.presentation

import com.jaroapps.dailybrain.feature.stroop.domain.model.StroopStimulus

data class StroopUiState(
    val stimuli: List<StroopStimulus> = emptyList(),
    val elapsedTimeMs: Long = 0L,
    val isFinished: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
)
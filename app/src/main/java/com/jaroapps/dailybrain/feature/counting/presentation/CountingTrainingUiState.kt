package com.jaroapps.dailybrain.feature.counting.presentation

data class CountingTrainingUiState(
    val elapsedTimeMs: Long = 0L,
    val isFinished: Boolean = false,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val errorMessage: String? = null,
)
package com.jaroapps.dailybrain.feature.memory.presentation

import com.jaroapps.dailybrain.feature.memory.domain.model.MemoryTrainingResult
import com.jaroapps.dailybrain.feature.memory.domain.model.MemoryWord

sealed interface MemoryTrainingUiState {

    data object Loading : MemoryTrainingUiState

    data class Memorizing(
        val words: List<MemoryWord>,
        val progress: Float = 0f
    ) : MemoryTrainingUiState

    data class Recall(
        val words: List<MemoryWord>,
        val answers: List<String>,
        val isSaving: Boolean = false,
        val errorMessage: String? = null
    ) : MemoryTrainingUiState

    data class Result(
        val result: MemoryTrainingResult
    ) : MemoryTrainingUiState

    data class Error(
        val message: String
    ) : MemoryTrainingUiState
}

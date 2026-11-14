package com.jaroapps.dailybrain.feature.numbergrid.presentation

import com.jaroapps.dailybrain.feature.numbergrid.domain.model.NumberGridRules

data class NumberGridUiState(
    val gridSize: Int = NumberGridRules.DEFAULT_GRID_SIZE,
    val numbers: List<Int> = emptyList(),
    val expectedNumber: Int = 1,
    val lastCorrectNumber: Int? = null,
    val errorCount: Int = 0,
    val elapsedTimeMs: Long = 0L,
    val isStarted: Boolean = false,
    val isCompleted: Boolean = false,
    val durationMs: Long? = null,
    val accuracyPercent: Double? = null,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val saveErrorMessage: String? = null,
) {
    val totalCellCount: Int
        get() = numbers.size

    val progress: Float
        get() = if (totalCellCount > 0) {
            (expectedNumber - 1).toFloat() / totalCellCount
        } else {
            0f
        }
}

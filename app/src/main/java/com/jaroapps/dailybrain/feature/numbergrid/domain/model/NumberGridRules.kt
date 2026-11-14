package com.jaroapps.dailybrain.feature.numbergrid.domain.model

import kotlin.random.Random

/**
 * Pure game rules for the number-grid exercise.
 * The same implementation supports 3×3, 4×4, 5×5, and larger square grids.
 */
object NumberGridRules {
    const val DEFAULT_GRID_SIZE = 5

    fun generateNumbers(
        gridSize: Int = DEFAULT_GRID_SIZE,
        random: Random = Random.Default,
    ): List<Int> {
        require(gridSize >= 3) { "Grid size must be at least 3×3" }
        val cellCount = Math.multiplyExact(gridSize, gridSize)
        return (1..cellCount).shuffled(random)
    }

    fun isCorrectSelection(selectedNumber: Int, expectedNumber: Int): Boolean =
        selectedNumber == expectedNumber

    fun accuracyPercent(errorCount: Int, totalCellCount: Int): Double {
        require(totalCellCount > 0) { "Grid must contain at least one cell" }
        val normalizedErrors = errorCount.coerceAtLeast(0)
        return (100.0 - normalizedErrors * 100.0 / totalCellCount).coerceIn(0.0, 100.0)
    }
}

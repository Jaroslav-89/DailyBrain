package com.jaroapps.dailybrain.feature.arithmetic.presentation

import com.jaroapps.dailybrain.feature.arithmetic.domain.model.ArithmeticOperation
import com.jaroapps.dailybrain.feature.arithmetic.domain.model.ArithmeticProblem
import com.jaroapps.dailybrain.feature.arithmetic.domain.model.ArithmeticTaskResult

data class ArithmeticUiState(
    val operation: ArithmeticOperation = ArithmeticOperation.ADDITION,
    val currentProblem: ArithmeticProblem? = null,
    val totalTaskCount: Int = 100,
    val completedTaskCount: Int = 0,
    val answerInput: String = "",
    val isStarted: Boolean = false,
    val isCompleted: Boolean = false,
    val durationMs: Long? = null,
    val accuracyPercent: Double? = null,
    val taskResults: List<ArithmeticTaskResult> = emptyList(),
) {
    val progress: Float
        get() = if (totalTaskCount > 0) {
            completedTaskCount.toFloat() / totalTaskCount
        } else {
            0f
        }

    val currentTaskNumber: Int
        get() = (completedTaskCount + 1).coerceAtMost(totalTaskCount)

    val incorrectAnswers: List<ArithmeticTaskResult>
        get() = taskResults.filterNot { it.isCorrect }
}
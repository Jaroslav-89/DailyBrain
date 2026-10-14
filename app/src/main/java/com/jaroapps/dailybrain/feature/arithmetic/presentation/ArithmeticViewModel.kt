package com.jaroapps.dailybrain.feature.arithmetic.presentation

import androidx.lifecycle.ViewModel
import com.jaroapps.dailybrain.feature.arithmetic.domain.model.ArithmeticOperation
import com.jaroapps.dailybrain.feature.arithmetic.domain.model.ArithmeticProblem
import com.jaroapps.dailybrain.feature.arithmetic.domain.model.ArithmeticTaskResult
import com.jaroapps.dailybrain.feature.arithmetic.domain.model.ArithmeticTrainingConfig
import com.jaroapps.dailybrain.feature.arithmetic.domain.usecase.CheckArithmeticAnswerUseCase
import com.jaroapps.dailybrain.feature.arithmetic.domain.usecase.GenerateArithmeticProblemUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ArithmeticViewModel(
    private val generateProblem: GenerateArithmeticProblemUseCase,
    private val checkAnswer: CheckArithmeticAnswerUseCase,

    ) : ViewModel() {

    private val _uiState = MutableStateFlow(ArithmeticUiState())
    val uiState: StateFlow<ArithmeticUiState> = _uiState.asStateFlow()

    private var startedAtMs: Long? = null

    fun startTraining(
        operation: ArithmeticOperation = ArithmeticOperation.ADDITION,
        config: ArithmeticTrainingConfig = ArithmeticTrainingConfig(),
    ) {
        startedAtMs = System.currentTimeMillis()

        _uiState.value = ArithmeticUiState(
            operation = operation,
            currentProblem = generateProblem.execute(operation),
            totalTaskCount = config.taskCount,
            isStarted = true,
        )
    }

    fun onDigitPressed(digit: Int) {
        if (digit !in 0..9) return

        val state = _uiState.value
        if (!state.isStarted || state.isCompleted) return

        // Не допускаем неограниченный ввод.
        if (state.answerInput.length >= 2) return

        // Не допускаем ведущие нули вроде 007.
        if (state.answerInput == "0") return

        _uiState.update {
            it.copy(answerInput = it.answerInput + digit)
        }
    }

    fun onDeletePressed() {
        val state = _uiState.value
        if (!state.isStarted || state.isCompleted) return

        _uiState.update {
            it.copy(answerInput = it.answerInput.dropLast(1))
        }
    }

    fun submitAnswer() {
        val state = _uiState.value
        val problem = state.currentProblem ?: return
        if (!state.isStarted || state.isCompleted) return
        if (state.answerInput.isBlank()) return

        val answer = state.answerInput.toIntOrNull() ?: return


        val result = ArithmeticTaskResult(
            problem = problem,
            userAnswer = answer,
        )

        val results = state.taskResults + result
        val completedCount = state.completedTaskCount + 1

        if (completedCount == state.totalTaskCount) {
            finishTraining(
                state = state,
                results = results,
                completedCount = completedCount,
            )
        } else {
            _uiState.value = state.copy(
                currentProblem = generateProblem.execute(state.operation),
                answerInput = "",
                completedTaskCount = completedCount,
                taskResults = results,
            )
        }
    }

    fun cancelTraining() {
        startedAtMs = null
        _uiState.value = ArithmeticUiState()
    }

    private fun finishTraining(
        state: ArithmeticUiState,
        results: List<ArithmeticTaskResult>,
        completedCount: Int,
    ) {
        val startedAt = startedAtMs ?: System.currentTimeMillis()
        val duration = (System.currentTimeMillis() - startedAt)
            .coerceAtLeast(0L)

        val correctCount = results.count { it.isCorrect }
        val accuracy = correctCount * 100.0 / results.size

        _uiState.value = state.copy(
            completedTaskCount = completedCount,
            taskResults = results,
            isCompleted = true,
            durationMs = duration,
            accuracyPercent = accuracy,
            answerInput = "",
        )

        startedAtMs = null
    }
}
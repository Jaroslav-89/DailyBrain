package com.jaroapps.dailybrain.feature.arithmetic.presentation

import androidx.lifecycle.ViewModel
import com.jaroapps.dailybrain.feature.arithmetic.domain.model.ArithmeticTaskResult
import com.jaroapps.dailybrain.feature.arithmetic.domain.model.ArithmeticTrainingConfig
import com.jaroapps.dailybrain.feature.arithmetic.domain.usecase.CheckArithmeticAnswerUseCase
import com.jaroapps.dailybrain.feature.arithmetic.domain.usecase.GenerateArithmeticProblemUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import androidx.lifecycle.viewModelScope
import com.jaroapps.dailybrain.common.domain.model.TrainingSession
import com.jaroapps.dailybrain.feature.arithmetic.domain.repository.ArithmeticTrainingRepository
import kotlinx.coroutines.launch

class ArithmeticViewModel(
    private val generateProblem: GenerateArithmeticProblemUseCase,
    private val checkAnswer: CheckArithmeticAnswerUseCase,
    private val arithmeticTrainingRepository: ArithmeticTrainingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ArithmeticUiState())
    val uiState: StateFlow<ArithmeticUiState> = _uiState.asStateFlow()

    private var startedAtMs: Long? = null

    fun startTraining(
        config: ArithmeticTrainingConfig = ArithmeticTrainingConfig(taskCount = 50),
    ) {
        startedAtMs = System.currentTimeMillis()

        val firstProblem = generateProblem.execute()

        _uiState.value = ArithmeticUiState(
            operation = firstProblem.operation,
            currentProblem = firstProblem,
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
            val nextProblem = generateProblem.execute()

            _uiState.value = state.copy(
                operation = nextProblem.operation,
                currentProblem = nextProblem,
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
        val finishedAt = System.currentTimeMillis()
        val startedAt = startedAtMs ?: finishedAt

        val duration = (finishedAt - startedAt).coerceAtLeast(0L)
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

        viewModelScope.launch {
            arithmeticTrainingRepository.saveCompletedTraining(
                TrainingSession(
                    exerciseType = "ARITHMETIC",
                    startedAtEpochMs = startedAt,
                    finishedAtEpochMs = finishedAt,
                    durationMs = duration,
                    accuracyPercent = accuracy,
                    taskCount = completedCount,
                    correctCount = correctCount,
                )
            )
        }

        startedAtMs = null
    }
}
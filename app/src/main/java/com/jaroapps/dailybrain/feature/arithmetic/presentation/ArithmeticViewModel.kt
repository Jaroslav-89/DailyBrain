package com.jaroapps.dailybrain.feature.arithmetic.presentation

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jaroapps.dailybrain.common.domain.model.ExerciseType
import com.jaroapps.dailybrain.common.domain.model.TrainingSession
import com.jaroapps.dailybrain.feature.arithmetic.domain.model.ArithmeticTaskResult
import com.jaroapps.dailybrain.feature.arithmetic.domain.model.ArithmeticTrainingConfig
import com.jaroapps.dailybrain.common.domain.usecase.SaveCompletedTrainingUseCase
import com.jaroapps.dailybrain.feature.arithmetic.domain.usecase.CheckArithmeticAnswerUseCase
import com.jaroapps.dailybrain.feature.arithmetic.domain.usecase.GenerateArithmeticProblemUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ArithmeticViewModel(
    private val generateProblem: GenerateArithmeticProblemUseCase,
    private val checkAnswer: CheckArithmeticAnswerUseCase,
    private val saveCompletedTraining: SaveCompletedTrainingUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ArithmeticUiState())
    val uiState: StateFlow<ArithmeticUiState> = _uiState.asStateFlow()

    private var startedAtEpochMs: Long? = null
    private var elapsedBeforePauseMs = 0L
    private var activeSegmentStartedAtMs: Long? = null
    private var isForeground = false
    private var pendingSession: TrainingSession? = null
    private var saveInProgress = false

    fun startTraining(
        config: ArithmeticTrainingConfig = ArithmeticTrainingConfig(taskCount = 10),
    ) {
        startedAtEpochMs = System.currentTimeMillis()
        elapsedBeforePauseMs = 0L
        activeSegmentStartedAtMs = if (isForeground) SystemClock.elapsedRealtime() else null
        pendingSession = null
        saveInProgress = false

        val firstProblem = generateProblem.execute()

        _uiState.value = ArithmeticUiState(
            operation = firstProblem.operation,
            currentProblem = firstProblem,
            totalTaskCount = config.taskCount,
            isStarted = true,
        )
    }

    fun onAppResumed() {
        isForeground = true
        if (_uiState.value.isStarted && !_uiState.value.isCompleted && activeSegmentStartedAtMs == null) {
            activeSegmentStartedAtMs = SystemClock.elapsedRealtime()
        }
    }

    fun onAppPaused() {
        isForeground = false
        pauseActiveSegment()
    }

    private fun pauseActiveSegment() {
        val segmentStartedAt = activeSegmentStartedAtMs ?: return
        elapsedBeforePauseMs +=
            (SystemClock.elapsedRealtime() - segmentStartedAt).coerceAtLeast(0L)
        activeSegmentStartedAtMs = null
    }

    private fun currentDurationMs(): Long {
        val activeSegment = activeSegmentStartedAtMs?.let { startedAt ->
            (SystemClock.elapsedRealtime() - startedAt).coerceAtLeast(0L)
        } ?: 0L
        return elapsedBeforePauseMs + activeSegment
    }

    fun onDigitPressed(digit: Int) {
        if (digit !in 0..9) return

        val state = _uiState.value
        if (!state.isStarted || state.isCompleted) return
        if (state.answerInput.length >= 2) return
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
        // Не теряем неподтверждённый результат при случайном сбросе экрана.
        if (_uiState.value.isCompleted && !_uiState.value.isSaved) return

        startedAtEpochMs = null
        elapsedBeforePauseMs = 0L
        activeSegmentStartedAtMs = null
        pendingSession = null
        _uiState.value = ArithmeticUiState()
    }

    private fun finishTraining(
        state: ArithmeticUiState,
        results: List<ArithmeticTaskResult>,
        completedCount: Int,
    ) {
        if (_uiState.value.isCompleted) return

        val finishedAt = System.currentTimeMillis()
        val startedAt = startedAtEpochMs ?: finishedAt
        val duration = currentDurationMs()
        pauseActiveSegment()
        val correctCount = results.count { it.isCorrect }
        val accuracy = correctCount * 100.0 / results.size

        pendingSession = TrainingSession(
            exerciseType = ExerciseType.ARITHMETIC.storageValue,
            startedAtEpochMs = startedAt,
            finishedAtEpochMs = finishedAt,
            durationMs = duration,
            accuracyPercent = accuracy,
            taskCount = completedCount,
            correctCount = correctCount,
        )

        _uiState.value = state.copy(
            completedTaskCount = completedCount,
            taskResults = results,
            isCompleted = true,
            durationMs = duration,
            accuracyPercent = accuracy,
            answerInput = "",
            isSaving = true,
            isSaved = false,
            saveErrorMessage = null,
        )

        startedAtEpochMs = null
        startedAtElapsedMs = null
        savePendingSession()
    }

    fun retrySave() {
        if (!_uiState.value.isCompleted || _uiState.value.isSaved || saveInProgress) return
        savePendingSession()
    }

    private fun savePendingSession() {
        val session = pendingSession ?: return
        if (saveInProgress) return

        saveInProgress = true
        _uiState.update {
            it.copy(isSaving = true, saveErrorMessage = null)
        }

        viewModelScope.launch {
            try {
                saveCompletedTraining(session)
                _uiState.update {
                    it.copy(isSaving = false, isSaved = true, saveErrorMessage = null)
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        isSaved = false,
                        saveErrorMessage = exception.message
                            ?: "Не удалось сохранить результат. Попробуй ещё раз.",
                    )
                }
            } finally {
                saveInProgress = false
            }
        }
    }

    fun openAnswerReview() {
        if (_uiState.value.isCompleted) {
            _uiState.update {
                it.copy(isReviewingAnswers = true)
            }
        }
    }

    fun closeAnswerReview() {
        _uiState.update {
            it.copy(isReviewingAnswers = false)
        }
    }
}
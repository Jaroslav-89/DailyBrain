package com.jaroapps.dailybrain.feature.counting.presentation

import com.jaroapps.dailybrain.core.time.ActiveTimeTracker
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jaroapps.dailybrain.common.domain.model.ExerciseType
import com.jaroapps.dailybrain.common.domain.model.TrainingSession
import com.jaroapps.dailybrain.common.domain.usecase.SaveCompletedTrainingUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CountingTrainingViewModel(
    private val saveCompletedTraining: SaveCompletedTrainingUseCase,
) : ViewModel() {

    companion object {
        private const val TIMER_UPDATE_INTERVAL_MS = 100L
    }

    private val _uiState = MutableStateFlow(CountingTrainingUiState())
    val uiState = _uiState.asStateFlow()

    private var startedAtEpochMs: Long? = null

    private var timerJob: Job? = null
    private val activeTimeTracker = ActiveTimeTracker()
    private var pendingSession: TrainingSession? = null
    private var saveInProgress = false

    fun onAppResumed() {
        if (!_uiState.value.isFinished) {
            startTimer()
        }
    }

    fun onAppPaused() {
        pauseTimer()
    }

    private fun startTimer() {
        if (timerJob?.isActive == true || _uiState.value.isFinished) {
            return
        }

        if (startedAtEpochMs == null) startedAtEpochMs = System.currentTimeMillis()
        activeTimeTracker.resume()

        timerJob = viewModelScope.launch {
            while (true) {
                _uiState.value = _uiState.value.copy(
                    elapsedTimeMs = activeTimeTracker.elapsedMs(),
                )

                delay(TIMER_UPDATE_INTERVAL_MS)
            }
        }
    }

    private fun pauseTimer() {
        timerJob?.cancel()
        timerJob = null
        activeTimeTracker.pause()

        _uiState.value = _uiState.value.copy(
            elapsedTimeMs = activeTimeTracker.elapsedMs(),
        )
    }

    fun finishTraining() {
        if (_uiState.value.isFinished || saveInProgress) return

        pauseTimer()

        val finishedAtEpochMs = System.currentTimeMillis()

        // Фиксируем один и тот же результат для первой попытки
        // сохранения и всех возможных повторных попыток.
        pendingSession = TrainingSession(
            exerciseType = ExerciseType.COUNTING_TRAINING.storageValue,
            startedAtEpochMs = startedAtEpochMs ?: finishedAtEpochMs,
            finishedAtEpochMs = finishedAtEpochMs,
            durationMs = activeTimeTracker.elapsedMs(),
            accuracyPercent = 0.0,
            taskCount = 0,
            correctCount = 0,
        )

        _uiState.value = _uiState.value.copy(
            elapsedTimeMs = activeTimeTracker.elapsedMs(),
            isFinished = true,
            isSaving = true,
            isSaved = false,
            errorMessage = null,
        )

        savePendingSession()
    }

    fun retrySave() {
        if (!_uiState.value.isFinished || saveInProgress) return
        if (_uiState.value.isSaved) return

        savePendingSession()
    }

    private fun savePendingSession() {
        val session = pendingSession ?: return
        if (saveInProgress) return

        saveInProgress = true
        _uiState.value = _uiState.value.copy(
            isSaving = true,
            errorMessage = null,
        )

        viewModelScope.launch {
            try {
                saveCompletedTraining(session)

                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    isSaved = true,
                    errorMessage = null,
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    isSaved = false,
                    errorMessage = exception.message
                        ?: "Не удалось сохранить результат. Попробуй ещё раз.",
                )
            } finally {
                saveInProgress = false
            }
        }
    }

    override fun onCleared() {
        timerJob?.cancel()
        super.onCleared()
    }
}
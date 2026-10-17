package com.jaroapps.dailybrain.feature.stroop.presentation

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jaroapps.dailybrain.common.domain.model.TrainingSession
import com.jaroapps.dailybrain.common.domain.model.ExerciseType
import com.jaroapps.dailybrain.feature.arithmetic.domain.repository.ArithmeticTrainingRepository
import com.jaroapps.dailybrain.feature.stroop.domain.usecase.GenerateStroopStimuliUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class StroopViewModel(
    private val generateStimuli: GenerateStroopStimuliUseCase,
    private val repository: ArithmeticTrainingRepository,
) : ViewModel() {

    companion object {
        private const val TIMER_UPDATE_INTERVAL_MS = 100L
    }

    private val _uiState = MutableStateFlow(
        StroopUiState(
            stimuli = generateStimuli(),
        )
    )
    val uiState = _uiState.asStateFlow()

    private val startedAtEpochMs = System.currentTimeMillis()

    private var timerJob: Job? = null
    private var isForeground = false
    private var elapsedBeforePauseMs = 0L
    private var activeSegmentStartedAtMs = 0L
    private var saveInProgress = false

    fun onAppResumed() {
        isForeground = true
        if (!_uiState.value.isFinished) startTimer()
    }

    fun onAppPaused() {
        isForeground = false
        pauseTimer()
    }

    private fun startTimer() {
        if (timerJob?.isActive == true || _uiState.value.isFinished) return

        activeSegmentStartedAtMs = SystemClock.elapsedRealtime()

        timerJob = viewModelScope.launch {
            while (true) {
                val currentSegmentMs =
                    SystemClock.elapsedRealtime() - activeSegmentStartedAtMs

                val elapsedMs = elapsedBeforePauseMs + currentSegmentMs

                _uiState.value = _uiState.value.copy(
                    elapsedTimeMs = elapsedMs,
                )

                delay(TIMER_UPDATE_INTERVAL_MS)
            }
        }
    }

    private fun pauseTimer() {
        val job = timerJob ?: return

        if (job.isActive) {
            elapsedBeforePauseMs +=
                SystemClock.elapsedRealtime() - activeSegmentStartedAtMs
        }

        job.cancel()
        timerJob = null

        _uiState.value = _uiState.value.copy(
            elapsedTimeMs = elapsedBeforePauseMs,
        )
    }

    fun finishTraining() {
        val current = _uiState.value
        if (current.isFinished || saveInProgress) return

        pauseTimer()
        saveInProgress = true

        val finishedAtEpochMs = System.currentTimeMillis()
        val durationMs = elapsedBeforePauseMs

        _uiState.value = current.copy(
            elapsedTimeMs = durationMs,
            isFinished = true,
            isSaving = true,
            errorMessage = null,
        )

        viewModelScope.launch {
            try {
                repository.saveCompletedTraining(
                    TrainingSession(
                        exerciseType = ExerciseType.STROOP_TEST.storageValue,
                        startedAtEpochMs = startedAtEpochMs,
                        finishedAtEpochMs = finishedAtEpochMs,
                        durationMs = durationMs,
                        accuracyPercent = 0.0,
                        taskCount = 0,
                        correctCount = 0,
                    )
                )

                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                )
            } catch (exception: Exception) {
                // Allow the user to retry saving without rerunning the test.
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    errorMessage = exception.message
                        ?: "Не удалось сохранить результат. Попробуй ещё раз.",
                )
            } finally {
                saveInProgress = false
            }
        }
    }

    fun retrySave() {
        val current = _uiState.value
        if (!current.isFinished || saveInProgress) return

        saveInProgress = true
        val finishedAtEpochMs = System.currentTimeMillis()

        _uiState.value = current.copy(
            isSaving = true,
            errorMessage = null,
        )

        viewModelScope.launch {
            try {
                repository.saveCompletedTraining(
                    TrainingSession(
                        exerciseType = ExerciseType.STROOP_TEST.storageValue,
                        startedAtEpochMs = startedAtEpochMs,
                        finishedAtEpochMs = finishedAtEpochMs,
                        durationMs = _uiState.value.elapsedTimeMs,
                        accuracyPercent = 0.0,
                        taskCount = 0,
                        correctCount = 0,
                    )
                )
                _uiState.value = _uiState.value.copy(isSaving = false)
            } catch (exception: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
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
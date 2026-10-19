package com.jaroapps.dailybrain.feature.stroop.presentation

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jaroapps.dailybrain.common.domain.model.TrainingSession
import com.jaroapps.dailybrain.common.domain.model.ExerciseType
import com.jaroapps.dailybrain.common.domain.usecase.SaveCompletedTrainingUseCase
import com.jaroapps.dailybrain.feature.stroop.domain.usecase.GenerateStroopStimuliUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class StroopViewModel(
    private val generateStimuli: GenerateStroopStimuliUseCase,
    private val saveCompletedTraining: SaveCompletedTrainingUseCase,
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
    private var pendingSession: TrainingSession? = null

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
                _uiState.value = _uiState.value.copy(elapsedTimeMs = elapsedMs)
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
        val finishedAtEpochMs = System.currentTimeMillis()
        val durationMs = elapsedBeforePauseMs

        // Фиксируем сессию один раз: повторная попытка сохраняет те же значения.
        pendingSession = TrainingSession(
            exerciseType = ExerciseType.STROOP_TEST.storageValue,
            startedAtEpochMs = startedAtEpochMs,
            finishedAtEpochMs = finishedAtEpochMs,
            durationMs = durationMs,
            accuracyPercent = 0.0,
            taskCount = 0,
            correctCount = 0,
        )

        _uiState.value = current.copy(
            elapsedTimeMs = durationMs,
            isFinished = true,
            isSaving = true,
            isSaved = false,
            errorMessage = null,
        )
        savePendingSession()
    }

    fun retrySave() {
        val current = _uiState.value
        if (!current.isFinished || current.isSaved || saveInProgress) return
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

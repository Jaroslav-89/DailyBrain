package com.jaroapps.dailybrain.feature.numbergrid.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jaroapps.dailybrain.R
import com.jaroapps.dailybrain.common.domain.model.ExerciseType
import com.jaroapps.dailybrain.common.domain.model.TrainingSession
import com.jaroapps.dailybrain.common.domain.usecase.SaveCompletedTrainingUseCase
import com.jaroapps.dailybrain.core.resources.StringProvider
import com.jaroapps.dailybrain.core.time.ActiveTimeTracker
import com.jaroapps.dailybrain.core.time.elapsedTimeUpdates
import com.jaroapps.dailybrain.feature.numbergrid.domain.model.NumberGridRules
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class NumberGridViewModel(
    private val saveCompletedTraining: SaveCompletedTrainingUseCase,
    private val strings: StringProvider,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NumberGridUiState())
    val uiState = _uiState.asStateFlow()

    private val activeTimeTracker = ActiveTimeTracker()
    private var timerJob: Job? = null
    private var startedAtEpochMs: Long? = null
    private var pendingSession: TrainingSession? = null
    private var isForeground = false
    private var saveInProgress = false

    fun startTraining(gridSize: Int = NumberGridRules.DEFAULT_GRID_SIZE) {
        require(gridSize >= 3) { "Grid size must be at least 3×3" }

        timerJob?.cancel()
        timerJob = null
        activeTimeTracker.reset()
        startedAtEpochMs = System.currentTimeMillis()
        pendingSession = null
        saveInProgress = false

        _uiState.value = NumberGridUiState(
            gridSize = gridSize,
            numbers = NumberGridRules.generateNumbers(gridSize),
            isStarted = true,
        )
        if (isForeground) resumeTimer()
    }

    fun onAppResumed() {
        isForeground = true
        if (_uiState.value.isStarted && !_uiState.value.isCompleted) {
            resumeTimer()
        }
    }

    fun onAppPaused() {
        isForeground = false
        pauseTimer()
    }

    fun onNumberSelected(number: Int) {
        val current = _uiState.value
        if (!current.isStarted || current.isCompleted) return

        if (!NumberGridRules.isCorrectSelection(number, current.expectedNumber)) {
            _uiState.update { state ->
                if (state.isCompleted) state else state.copy(errorCount = state.errorCount + 1)
            }
            return
        }

        _uiState.value = current.copy(
            lastCorrectNumber = number,
            expectedNumber = number + 1,
        )
        if (number == current.totalCellCount) finishTraining()
    }

    fun retrySave() {
        if (!_uiState.value.isCompleted || _uiState.value.isSaved || saveInProgress) return
        savePendingSession()
    }

    fun cancelTraining() {
        pauseTimer()
        activeTimeTracker.reset()
        startedAtEpochMs = null
        pendingSession = null
        saveInProgress = false
        _uiState.value = NumberGridUiState()
    }

    private fun resumeTimer() {
        val state = _uiState.value
        if (!isForeground || !state.isStarted || state.isCompleted || timerJob?.isActive == true) return

        activeTimeTracker.resume()
        timerJob = viewModelScope.launch {
            activeTimeTracker.elapsedTimeUpdates().collect { elapsed ->
                _uiState.update { current ->
                    if (current.isStarted && !current.isCompleted) {
                        current.copy(elapsedTimeMs = elapsed)
                    } else {
                        current
                    }
                }
            }
        }
    }

    private fun pauseTimer() {
        timerJob?.cancel()
        timerJob = null
        activeTimeTracker.pause()
        _uiState.update { current ->
            if (current.isStarted && !current.isCompleted) {
                current.copy(elapsedTimeMs = activeTimeTracker.elapsedMs())
            } else {
                current
            }
        }
    }

    private fun finishTraining() {
        val current = _uiState.value
        if (!current.isStarted || current.isCompleted) return

        pauseTimer()
        val finishedAt = System.currentTimeMillis()
        val duration = activeTimeTracker.elapsedMs()
        val accuracy = NumberGridRules.accuracyPercent(current.errorCount, current.totalCellCount)
        val startedAt = startedAtEpochMs ?: finishedAt

        pendingSession = TrainingSession(
            exerciseType = ExerciseType.storageValueForGridSize(current.gridSize),
            startedAtEpochMs = startedAt,
            finishedAtEpochMs = finishedAt,
            durationMs = duration,
            accuracyPercent = accuracy,
            taskCount = current.totalCellCount,
            correctCount = current.totalCellCount,
        )

        _uiState.value = _uiState.value.copy(
            elapsedTimeMs = duration,
            isCompleted = true,
            durationMs = duration,
            accuracyPercent = accuracy,
            isSaving = true,
            isSaved = false,
            saveErrorMessage = null,
        )
        startedAtEpochMs = null
        savePendingSession()
    }

    private fun savePendingSession() {
        val session = pendingSession ?: return
        if (saveInProgress) return

        saveInProgress = true
        _uiState.update { it.copy(isSaving = true, saveErrorMessage = null) }

        viewModelScope.launch {
            try {
                saveCompletedTraining(session)
                _uiState.update {
                    it.copy(isSaving = false, isSaved = true, saveErrorMessage = null)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        isSaved = false,
                        saveErrorMessage = strings.getString(R.string.error_save_failed),
                    )
                }
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

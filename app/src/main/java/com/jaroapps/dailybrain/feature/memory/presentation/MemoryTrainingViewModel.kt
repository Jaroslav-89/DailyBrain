package com.jaroapps.dailybrain.feature.memory.presentation

import com.jaroapps.dailybrain.core.time.ActiveTimeTracker
import com.jaroapps.dailybrain.core.resources.StringProvider
import com.jaroapps.dailybrain.R
import com.jaroapps.dailybrain.core.time.elapsedTimeUpdates
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jaroapps.dailybrain.feature.memory.domain.model.MemoryTrainingResult
import com.jaroapps.dailybrain.feature.memory.domain.usecase.CheckMemoryAnswersUseCase
import com.jaroapps.dailybrain.feature.memory.domain.usecase.GetRandomMemoryWordsUseCase
import com.jaroapps.dailybrain.feature.memory.domain.usecase.SaveMemoryTrainingUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MemoryTrainingViewModel(
    private val getRandomMemoryWords: GetRandomMemoryWordsUseCase,
    private val checkMemoryAnswers: CheckMemoryAnswersUseCase,
    private val saveMemoryTraining: SaveMemoryTrainingUseCase,
    private val strings: StringProvider,
) : ViewModel() {

    companion object {
        const val MEMORIZATION_DURATION_MS = 120_000L
    }

    private val _uiState =
        MutableStateFlow<MemoryTrainingUiState>(
            MemoryTrainingUiState.Loading
        )
    val uiState = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var isForeground = false
    private val memorizationTimer = ActiveTimeTracker()
    private var startedAtEpochMs = 0L
    private var memorizationFinished = false
    private var pendingResult: MemoryTrainingResult? = null
    private var pendingFinishedAtEpochMs: Long? = null
    private var pendingDurationMs: Long? = null
    private val sessionTimer = ActiveTimeTracker()
    private var saveInProgress = false

    init {
        loadWords()
    }

    private fun loadWords() {
        viewModelScope.launch {
            try {
                val words = getRandomMemoryWords()

                memorizationTimer.reset()
                sessionTimer.reset()
                memorizationFinished = false
                pendingResult = null
                pendingFinishedAtEpochMs = null
                pendingDurationMs = null
                saveInProgress = false
                startedAtEpochMs = System.currentTimeMillis()
                if (isForeground) startSessionClock()

                _uiState.value =
                    MemoryTrainingUiState.Memorizing(words = words)

                if (isForeground) {
                    startTimer()
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _uiState.value = MemoryTrainingUiState.Error(
                    strings.getString(R.string.error_load_words)
                )
            }
        }
    }

    /**
     * Вызывается экраном, когда приложение становится активным.
     */
    fun onAppResumed() {
        isForeground = true
        startSessionClock()

        if (_uiState.value is MemoryTrainingUiState.Memorizing) {
            startTimer()
        }
    }

    /**
     * Вызывается, когда приложение уходит в фон
     * или экран перестаёт быть активным.
     */
    fun onAppPaused() {
        isForeground = false
        pauseTimer()
        pauseSessionClock()
    }

    private fun startSessionClock() {
        if (startedAtEpochMs == 0L || sessionTimer.isRunning) return
        if (_uiState.value is MemoryTrainingUiState.Result || _uiState.value is MemoryTrainingUiState.Error) return
        sessionTimer.resume()
    }

    private fun pauseSessionClock() {
        sessionTimer.pause()
    }

    private fun currentSessionDurationMs(): Long = sessionTimer.elapsedMs()

    private fun startTimer() {
        if (timerJob?.isActive == true || memorizationFinished) return

        memorizationTimer.resume()

        timerJob = viewModelScope.launch {
            memorizationTimer.elapsedTimeUpdates()
                .takeWhile { it < MEMORIZATION_DURATION_MS }
                .collect { elapsedMs ->
                    val currentState = _uiState.value
                    if (currentState is MemoryTrainingUiState.Memorizing) {
                        val progress = (
                            elapsedMs.toFloat() / MEMORIZATION_DURATION_MS
                        ).coerceIn(0f, 1f)
                        _uiState.value = currentState.copy(progress = progress)
                    }
                }

            val currentState = _uiState.value
            if (currentState is MemoryTrainingUiState.Memorizing &&
                memorizationTimer.elapsedMs() >= MEMORIZATION_DURATION_MS
            ) {
                memorizationTimer.pause()
                memorizationFinished = true
                timerJob = null
                _uiState.value = MemoryTrainingUiState.Recall(
                    words = currentState.words,
                    answers = List(currentState.words.size) { "" }
                )
            }
        }
    }

    private fun pauseTimer() {
        timerJob?.cancel()
        timerJob = null
        memorizationTimer.pause()
    }

    fun updateAnswer(index: Int, value: String) {
        val state = _uiState.value as? MemoryTrainingUiState.Recall
            ?: return

        if (state.isSaving) return
        if (index !in state.answers.indices) return

        val updatedAnswers = state.answers.toMutableList()
        updatedAnswers[index] = value

        _uiState.value = state.copy(
            answers = updatedAnswers,
            errorMessage = null
        )
    }

    fun checkAnswers() {
        val state = _uiState.value as? MemoryTrainingUiState.Recall
            ?: return

        if (saveInProgress || state.isSaving) return

        if (state.answers.none { it.isNotBlank() }) {
            _uiState.value = state.copy(
                errorMessage = strings.getString(R.string.error_enter_word)
            )
            return
        }

        val result = pendingResult ?: checkMemoryAnswers(
            shownWords = state.words,
            enteredAnswers = state.answers
        ).also {
            pendingResult = it
        }

        saveResult(result, state)
    }

    private fun saveResult(
        result: MemoryTrainingResult,
        previousState: MemoryTrainingUiState.Recall
    ) {
        if (saveInProgress) return
        saveInProgress = true

        val finishedAtEpochMs = pendingFinishedAtEpochMs
            ?: System.currentTimeMillis().also {
                pendingFinishedAtEpochMs = it
                pendingDurationMs = currentSessionDurationMs()
                pauseSessionClock()
            }
        val durationMs = pendingDurationMs ?: currentSessionDurationMs()

        _uiState.value = previousState.copy(
            isSaving = true,
            errorMessage = null
        )

        viewModelScope.launch {
            try {
                saveMemoryTraining(
                    startedAtEpochMs = startedAtEpochMs,
                    finishedAtEpochMs = finishedAtEpochMs,
                    durationMs = durationMs,
                    result = result
                )

                _uiState.value = MemoryTrainingUiState.Result(result)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _uiState.value = previousState.copy(
                    isSaving = false,
                    errorMessage = strings.getString(R.string.error_save_failed)
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

package com.jaroapps.dailybrain.feature.memory.presentation

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jaroapps.dailybrain.feature.memory.domain.model.MemoryTrainingResult
import com.jaroapps.dailybrain.feature.memory.domain.usecase.CheckMemoryAnswersUseCase
import com.jaroapps.dailybrain.feature.memory.domain.usecase.GetRandomMemoryWordsUseCase
import com.jaroapps.dailybrain.feature.memory.domain.usecase.SaveMemoryTrainingUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MemoryTrainingViewModel(
    private val getRandomMemoryWords: GetRandomMemoryWordsUseCase,
    private val checkMemoryAnswers: CheckMemoryAnswersUseCase,
    private val saveMemoryTraining: SaveMemoryTrainingUseCase
) : ViewModel() {

    companion object {
        const val MEMORIZATION_DURATION_MS = 120_000L
        private const val TIMER_UPDATE_INTERVAL_MS = 100L
    }

    private val _uiState =
        MutableStateFlow<MemoryTrainingUiState>(
            MemoryTrainingUiState.Loading
        )
    val uiState = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var isForeground = false
    private var elapsedBeforePauseMs = 0L
    private var activeSegmentStartedAtMs = 0L
    private var startedAtEpochMs = 0L
    private var memorizationFinished = false
    private var pendingResult: MemoryTrainingResult? = null
    private var saveInProgress = false

    init {
        loadWords()
    }

    private fun loadWords() {
        viewModelScope.launch {
            try {
                val words = getRandomMemoryWords()

                elapsedBeforePauseMs = 0L
                memorizationFinished = false
                pendingResult = null
                saveInProgress = false
                startedAtEpochMs = System.currentTimeMillis()

                _uiState.value =
                    MemoryTrainingUiState.Memorizing(words = words)

                if (isForeground) {
                    startTimer()
                }
            } catch (exception: Exception) {
                _uiState.value = MemoryTrainingUiState.Error(
                    exception.message ?: "Не удалось загрузить список слов"
                )
            }
        }
    }

    /**
     * Вызывается экраном, когда приложение становится активным.
     */
    fun onAppResumed() {
        isForeground = true

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
    }

    private fun startTimer() {
        if (timerJob?.isActive == true || memorizationFinished) return

        activeSegmentStartedAtMs = SystemClock.elapsedRealtime()

        timerJob = viewModelScope.launch {
            while (true) {
                val currentSegmentMs =
                    SystemClock.elapsedRealtime() - activeSegmentStartedAtMs

                val elapsedMs = elapsedBeforePauseMs + currentSegmentMs
                val progress = (
                        elapsedMs.toFloat() / MEMORIZATION_DURATION_MS
                        ).coerceIn(0f, 1f)

                val currentState = _uiState.value
                if (currentState !is MemoryTrainingUiState.Memorizing) {
                    break
                }

                _uiState.value = currentState.copy(progress = progress)

                if (elapsedMs >= MEMORIZATION_DURATION_MS) {
                    elapsedBeforePauseMs = MEMORIZATION_DURATION_MS
                    memorizationFinished = true
                    timerJob = null

                    _uiState.value = MemoryTrainingUiState.Recall(
                        words = currentState.words,
                        answers = List(currentState.words.size) { "" }
                    )
                    break
                }

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
                errorMessage = "Введи хотя бы одно слово"
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

        _uiState.value = previousState.copy(
            isSaving = true,
            errorMessage = null
        )

        viewModelScope.launch {
            try {
                val finishedAtEpochMs = System.currentTimeMillis()

                saveMemoryTraining(
                    startedAtEpochMs = startedAtEpochMs,
                    finishedAtEpochMs = finishedAtEpochMs,
                    result = result
                )

                _uiState.value = MemoryTrainingUiState.Result(result)
            } catch (exception: Exception) {
                _uiState.value = previousState.copy(
                    isSaving = false,
                    errorMessage = exception.message
                        ?: "Не удалось сохранить результат. Попробуй ещё раз."
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

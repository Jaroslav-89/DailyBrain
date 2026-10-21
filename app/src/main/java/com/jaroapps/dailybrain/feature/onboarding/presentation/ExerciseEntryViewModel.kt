package com.jaroapps.dailybrain.feature.onboarding.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jaroapps.dailybrain.feature.onboarding.data.OnboardingProgressRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface ExerciseEntryUiState {
    data object Loading : ExerciseEntryUiState
    data class DestinationReady(val route: String) : ExerciseEntryUiState
    data object Error : ExerciseEntryUiState
}

class ExerciseEntryViewModel(
    private val onboardingProgressRepository: OnboardingProgressRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow<ExerciseEntryUiState>(ExerciseEntryUiState.Loading)
    val uiState: StateFlow<ExerciseEntryUiState> = _uiState

    fun resolve(exerciseKey: String) {
        viewModelScope.launch {
            _uiState.value = ExerciseEntryUiState.Loading
            try {
                val progress = onboardingProgressRepository.getActiveProgress()
                val hasSeenIntro = when (exerciseKey) {
                    "arithmetic" -> progress.hasSeenArithmeticIntro
                    "memory" -> progress.hasSeenMemoryIntro
                    "counting" -> progress.hasSeenCountingIntro
                    "stroop" -> progress.hasSeenStroopIntro
                    else -> throw IllegalArgumentException("Unknown exercise key: $exerciseKey")
                }
                _uiState.value = ExerciseEntryUiState.DestinationReady(
                    if (hasSeenIntro) "exercise_rules/$exerciseKey"
                    else "exercise_intro/$exerciseKey",
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.value = ExerciseEntryUiState.Error
            }
        }
    }
}

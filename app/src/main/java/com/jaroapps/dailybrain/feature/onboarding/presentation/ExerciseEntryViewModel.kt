package com.jaroapps.dailybrain.feature.onboarding.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jaroapps.dailybrain.feature.onboarding.domain.usecase.ExerciseEntryDestination
import com.jaroapps.dailybrain.feature.onboarding.domain.usecase.GetExerciseEntryDestinationUseCase
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
    private val getExerciseEntryDestination: GetExerciseEntryDestinationUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow<ExerciseEntryUiState>(ExerciseEntryUiState.Loading)
    val uiState: StateFlow<ExerciseEntryUiState> = _uiState

    fun resolve(exerciseKey: String) {
        viewModelScope.launch {
            _uiState.value = ExerciseEntryUiState.Loading
            try {
                val destination = getExerciseEntryDestination(exerciseKey)
                val route = when (destination) {
                    ExerciseEntryDestination.INTRO -> "exercise_intro/$exerciseKey"
                    ExerciseEntryDestination.RULES -> "exercise_rules/$exerciseKey"
                }
                _uiState.value = ExerciseEntryUiState.DestinationReady(route)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.value = ExerciseEntryUiState.Error
            }
        }
    }
}

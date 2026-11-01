package com.jaroapps.dailybrain.feature.onboarding.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jaroapps.dailybrain.feature.onboarding.domain.usecase.GetStartupDestinationUseCase
import com.jaroapps.dailybrain.feature.onboarding.domain.usecase.StartupDestination
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface StartupUiState {
    data object Loading : StartupUiState
    data object AppIntroRequired : StartupUiState
    data object HomeReady : StartupUiState
    data object Error : StartupUiState
}

class StartupViewModel(
    private val getStartupDestination: GetStartupDestinationUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow<StartupUiState>(StartupUiState.Loading)
    val uiState: StateFlow<StartupUiState> = _uiState

    init {
        observeStartup()
    }

    fun resolveStartup() = observeStartup()

    private fun observeStartup() {
        viewModelScope.launch {
            _uiState.value = StartupUiState.Loading
            try {
                getStartupDestination().collect { destination ->
                    _uiState.value = when (destination) {
                        StartupDestination.APP_ONBOARDING -> StartupUiState.AppIntroRequired
                        StartupDestination.HOME -> StartupUiState.HomeReady
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.value = StartupUiState.Error
            }
        }
    }
}

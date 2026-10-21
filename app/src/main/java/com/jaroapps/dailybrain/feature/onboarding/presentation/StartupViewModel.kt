package com.jaroapps.dailybrain.feature.onboarding.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jaroapps.dailybrain.feature.onboarding.data.OnboardingProgressRepository
import com.jaroapps.dailybrain.feature.profile.domain.repository.UserProfileRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed interface StartupUiState {
    data object Loading : StartupUiState
    data object AppIntroRequired : StartupUiState
    data object HomeReady : StartupUiState
    data object Error : StartupUiState
}

class StartupViewModel(
    private val onboardingProgressRepository: OnboardingProgressRepository,
    private val userProfileRepository: UserProfileRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow<StartupUiState>(StartupUiState.Loading)
    val uiState: StateFlow<StartupUiState> = _uiState

    init {
        resolveStartup()
    }

    fun resolveStartup() {
        viewModelScope.launch {
            _uiState.value = StartupUiState.Loading
            try {
                userProfileRepository.observeProfile().first()
                val progress = onboardingProgressRepository.getActiveProgress()
                _uiState.value = if (progress.hasSeenAppIntro) {
                    StartupUiState.HomeReady
                } else {
                    StartupUiState.AppIntroRequired
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.value = StartupUiState.Error
            }
        }
    }
}

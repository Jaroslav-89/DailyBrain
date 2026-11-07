package com.jaroapps.dailybrain.feature.onboarding.domain.usecase

import com.jaroapps.dailybrain.feature.onboarding.domain.repository.OnboardingProgressRepository
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

enum class StartupDestination {
    PROFILE_SELECTION,
    APP_ONBOARDING,
    HOME,
}

class GetStartupDestinationUseCase(
    private val repository: OnboardingProgressRepository,
) {
    operator fun invoke() = repository.observeProfileSelectionRequired()
        .map { it == true }
        .distinctUntilChanged()
        .flatMapLatest { selectionRequired ->
            if (selectionRequired) {
                flowOf(StartupDestination.PROFILE_SELECTION)
            } else {
                repository.observeActiveProgress().map { progress ->
                    if (progress.hasSeenAppIntro) StartupDestination.HOME
                    else StartupDestination.APP_ONBOARDING
                }
            }
        }
}

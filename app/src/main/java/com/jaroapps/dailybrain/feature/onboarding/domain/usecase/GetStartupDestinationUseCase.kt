package com.jaroapps.dailybrain.feature.onboarding.domain.usecase

import com.jaroapps.dailybrain.feature.onboarding.domain.repository.OnboardingProgressRepository
import kotlinx.coroutines.flow.map

enum class StartupDestination {
    APP_ONBOARDING,
    HOME,
}

class GetStartupDestinationUseCase(
    private val repository: OnboardingProgressRepository,
) {
    operator fun invoke() = repository.observeActiveProgress().map { progress ->
        if (progress.hasSeenAppIntro) StartupDestination.HOME
        else StartupDestination.APP_ONBOARDING
    }
}

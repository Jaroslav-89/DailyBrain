package com.jaroapps.dailybrain.feature.onboarding.domain.usecase

import com.jaroapps.dailybrain.feature.onboarding.domain.repository.OnboardingProgressRepository

class MarkAppIntroCompleteUseCase(
    private val repository: OnboardingProgressRepository,
) {
    suspend operator fun invoke() = repository.markAppIntroComplete()
}

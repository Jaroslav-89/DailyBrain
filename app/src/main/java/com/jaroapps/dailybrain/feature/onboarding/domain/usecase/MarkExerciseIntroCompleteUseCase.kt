package com.jaroapps.dailybrain.feature.onboarding.domain.usecase

import com.jaroapps.dailybrain.feature.onboarding.domain.repository.OnboardingProgressRepository

class MarkExerciseIntroCompleteUseCase(
    private val repository: OnboardingProgressRepository,
) {
    suspend operator fun invoke(exerciseKey: String) =
        repository.markExerciseIntroComplete(exerciseKey)
}

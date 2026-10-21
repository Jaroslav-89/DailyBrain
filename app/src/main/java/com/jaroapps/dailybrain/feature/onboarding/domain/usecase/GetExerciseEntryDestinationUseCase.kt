package com.jaroapps.dailybrain.feature.onboarding.domain.usecase

import com.jaroapps.dailybrain.feature.onboarding.domain.repository.OnboardingProgressRepository

enum class ExerciseEntryDestination {
    INTRO,
    RULES,
}

class GetExerciseEntryDestinationUseCase(
    private val repository: OnboardingProgressRepository,
) {
    suspend operator fun invoke(exerciseKey: String): ExerciseEntryDestination {
        val progress = repository.getActiveProgress()
        val hasSeenIntro = when (exerciseKey) {
            "arithmetic" -> progress.hasSeenArithmeticIntro
            "memory" -> progress.hasSeenMemoryIntro
            "counting" -> progress.hasSeenCountingIntro
            "stroop" -> progress.hasSeenStroopIntro
            else -> throw IllegalArgumentException("Unknown exercise key: $exerciseKey")
        }
        return if (hasSeenIntro) ExerciseEntryDestination.RULES
        else ExerciseEntryDestination.INTRO
    }
}

package com.jaroapps.dailybrain.feature.onboarding.domain.model

data class OnboardingProgress(
    val localAccountId: String,
    val hasSeenAppIntro: Boolean = false,
    val hasSeenArithmeticIntro: Boolean = false,
    val hasSeenMemoryIntro: Boolean = false,
    val hasSeenCountingIntro: Boolean = false,
    val hasSeenStroopIntro: Boolean = false,
)

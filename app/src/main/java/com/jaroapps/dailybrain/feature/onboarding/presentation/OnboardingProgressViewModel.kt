package com.jaroapps.dailybrain.feature.onboarding.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jaroapps.dailybrain.feature.onboarding.domain.usecase.MarkAppIntroCompleteUseCase
import com.jaroapps.dailybrain.feature.onboarding.domain.usecase.MarkExerciseIntroCompleteUseCase
import kotlinx.coroutines.launch

class OnboardingProgressViewModel(
    private val markAppIntroComplete: MarkAppIntroCompleteUseCase,
    private val markExerciseIntroComplete: MarkExerciseIntroCompleteUseCase,
) : ViewModel() {
    fun markAppIntroComplete(onComplete: () -> Unit) {
        viewModelScope.launch {
            markAppIntroComplete()
            onComplete()
        }
    }

    fun markExerciseIntroComplete(exerciseKey: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            markExerciseIntroComplete(exerciseKey)
            onComplete()
        }
    }
}

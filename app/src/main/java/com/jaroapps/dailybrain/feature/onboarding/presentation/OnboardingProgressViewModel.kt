package com.jaroapps.dailybrain.feature.onboarding.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jaroapps.dailybrain.feature.onboarding.data.OnboardingProgressRepository
import kotlinx.coroutines.launch

class OnboardingProgressViewModel(
    private val repository: OnboardingProgressRepository,
) : ViewModel() {
    fun markAppIntroComplete(onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.markAppIntroComplete()
            onComplete()
        }
    }

    fun markExerciseIntroComplete(exerciseKey: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.markExerciseIntroComplete(exerciseKey)
            onComplete()
        }
    }
}

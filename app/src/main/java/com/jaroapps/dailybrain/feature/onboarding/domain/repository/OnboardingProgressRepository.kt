package com.jaroapps.dailybrain.feature.onboarding.domain.repository

import com.jaroapps.dailybrain.feature.onboarding.domain.model.OnboardingProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

interface OnboardingProgressRepository {
    fun observeActiveProgress(): Flow<OnboardingProgress>
    fun observeProfileSelectionRequired(): Flow<Boolean?>
    suspend fun getActiveProgress(): OnboardingProgress
    suspend fun markAppIntroComplete()
    suspend fun markExerciseIntroComplete(exerciseKey: String)
    suspend fun deleteProgress(localAccountId: String)
}

package com.jaroapps.dailybrain.feature.onboarding.domain.usecase

import com.jaroapps.dailybrain.feature.onboarding.domain.model.OnboardingProgress
import com.jaroapps.dailybrain.feature.onboarding.domain.repository.OnboardingProgressRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class GetStartupDestinationUseCaseTest {

    @Test
    fun profileSelectionIsReturnedWithoutWaitingForAnActiveProfile() = runBlocking {
        val repository = FakeOnboardingProgressRepository(
            selectionRequired = flowOf(true),
            activeProgress = emptyFlow(),
        )

        assertEquals(
            StartupDestination.PROFILE_SELECTION,
            GetStartupDestinationUseCase(repository)().first(),
        )
    }

    @Test
    fun onboardingDestinationIsResolvedWhenProfileSelectionIsNotRequired() = runBlocking {
        val repository = FakeOnboardingProgressRepository(
            selectionRequired = flowOf(false),
            activeProgress = flowOf(OnboardingProgress(localAccountId = "local-a")),
        )

        assertEquals(
            StartupDestination.APP_ONBOARDING,
            GetStartupDestinationUseCase(repository)().first(),
        )
    }

    private class FakeOnboardingProgressRepository(
        private val selectionRequired: Flow<Boolean?>,
        private val activeProgress: Flow<OnboardingProgress>,
    ) : OnboardingProgressRepository {
        override fun observeActiveProgress(): Flow<OnboardingProgress> = activeProgress
        override fun observeProfileSelectionRequired(): Flow<Boolean?> = selectionRequired
        override suspend fun getActiveProgress(): OnboardingProgress =
            error("Not used by the startup destination test.")
        override suspend fun markAppIntroComplete() = Unit
        override suspend fun markExerciseIntroComplete(exerciseKey: String) = Unit
        override suspend fun deleteProgress(localAccountId: String) = Unit
    }
}

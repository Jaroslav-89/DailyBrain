package com.jaroapps.dailybrain.feature.profile.domain.usecase

import com.jaroapps.dailybrain.feature.profile.domain.repository.ProfileAuthSessionManager
import com.jaroapps.dailybrain.feature.profile.domain.repository.UserProfileRepository
import com.jaroapps.dailybrain.common.domain.service.TrainingStateChangedNotifier

class SwitchActiveProfileUseCase(
    private val repository: UserProfileRepository,
    private val profileAuthSessionManager: ProfileAuthSessionManager,
    private val stateChangedNotifier: TrainingStateChangedNotifier = TrainingStateChangedNotifier {},
) {
    suspend operator fun invoke(localAccountId: String) {
        val profile = requireNotNull(repository.getProfile(localAccountId)) {
            "Local profile does not exist: $localAccountId"
        }
        profileAuthSessionManager.ensureAuthenticated(profile)
        repository.switchActiveProfile(localAccountId)
        stateChangedNotifier.onTrainingStateChanged()
    }
}

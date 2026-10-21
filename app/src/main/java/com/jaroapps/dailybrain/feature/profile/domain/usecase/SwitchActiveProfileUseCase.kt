package com.jaroapps.dailybrain.feature.profile.domain.usecase

import com.jaroapps.dailybrain.feature.profile.domain.repository.UserProfileRepository
import com.jaroapps.dailybrain.common.domain.service.TrainingStateChangedNotifier

class SwitchActiveProfileUseCase(
    private val repository: UserProfileRepository,
    private val stateChangedNotifier: TrainingStateChangedNotifier = TrainingStateChangedNotifier {},
) {
    suspend operator fun invoke(localAccountId: String) {
        repository.switchActiveProfile(localAccountId)
        stateChangedNotifier.onTrainingStateChanged()
    }
}
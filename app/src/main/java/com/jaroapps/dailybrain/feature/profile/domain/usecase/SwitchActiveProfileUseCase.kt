package com.jaroapps.dailybrain.feature.profile.domain.usecase

import com.jaroapps.dailybrain.feature.profile.domain.repository.UserProfileRepository

class SwitchActiveProfileUseCase(
    private val repository: UserProfileRepository,
) {
    suspend operator fun invoke(localAccountId: String) =
        repository.switchActiveProfile(localAccountId)
}
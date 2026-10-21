package com.jaroapps.dailybrain.feature.profile.domain.usecase

import com.jaroapps.dailybrain.feature.profile.domain.model.UserProfile
import com.jaroapps.dailybrain.feature.profile.domain.repository.UserProfileRepository

class CreateLocalProfileUseCase(
    private val repository: UserProfileRepository,
) {
    suspend operator fun invoke(): UserProfile = repository.createLocalProfile()
}
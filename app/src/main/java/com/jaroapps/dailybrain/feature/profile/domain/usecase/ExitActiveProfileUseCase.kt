package com.jaroapps.dailybrain.feature.profile.domain.usecase

import com.jaroapps.dailybrain.feature.profile.domain.repository.UserProfileRepository

class ExitActiveProfileUseCase(
    private val repository: UserProfileRepository,
) {
    suspend operator fun invoke() = repository.exitActiveProfile()
}

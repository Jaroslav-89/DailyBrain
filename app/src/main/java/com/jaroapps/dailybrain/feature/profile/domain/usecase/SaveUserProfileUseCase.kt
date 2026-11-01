package com.jaroapps.dailybrain.feature.profile.domain.usecase

import com.jaroapps.dailybrain.feature.profile.domain.repository.UserProfileRepository

class SaveUserProfileUseCase(
    private val repository: UserProfileRepository,
) {
    suspend operator fun invoke(
        displayName: String?,
        gender: String?,
        birthDate: String?,
        country: String?,
        city: String?,
    ) = repository.saveProfile(displayName, gender, birthDate, country, city)
}

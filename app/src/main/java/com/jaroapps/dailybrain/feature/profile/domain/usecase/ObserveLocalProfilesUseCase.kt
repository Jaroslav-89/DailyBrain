package com.jaroapps.dailybrain.feature.profile.domain.usecase

import com.jaroapps.dailybrain.feature.profile.domain.model.UserProfile
import com.jaroapps.dailybrain.feature.profile.domain.repository.UserProfileRepository
import kotlinx.coroutines.flow.Flow

class ObserveLocalProfilesUseCase(
    private val repository: UserProfileRepository,
) {
    operator fun invoke(): Flow<List<UserProfile>> = repository.observeProfiles()
}
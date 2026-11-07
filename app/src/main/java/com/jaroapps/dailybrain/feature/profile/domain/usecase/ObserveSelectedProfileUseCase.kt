package com.jaroapps.dailybrain.feature.profile.domain.usecase

import com.jaroapps.dailybrain.feature.profile.domain.model.UserProfile
import com.jaroapps.dailybrain.feature.profile.domain.repository.UserProfileRepository
import kotlinx.coroutines.flow.Flow

/** Emits null while the user is on the profile selector. */
class ObserveSelectedProfileUseCase(
    private val repository: UserProfileRepository,
) {
    operator fun invoke(): Flow<UserProfile?> = repository.observeSelectedProfile()
}

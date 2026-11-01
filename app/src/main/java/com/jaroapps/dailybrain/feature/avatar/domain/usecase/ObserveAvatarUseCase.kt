package com.jaroapps.dailybrain.feature.avatar.domain.usecase

import com.jaroapps.dailybrain.feature.avatar.domain.model.AvatarConfiguration
import com.jaroapps.dailybrain.feature.avatar.domain.repository.AvatarRepository
import kotlinx.coroutines.flow.Flow

class ObserveAvatarUseCase(
    private val repository: AvatarRepository,
) {
    operator fun invoke(): Flow<AvatarConfiguration> = repository.observeAvatar()
}

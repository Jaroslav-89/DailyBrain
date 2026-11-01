package com.jaroapps.dailybrain.feature.avatar.domain.usecase

import com.jaroapps.dailybrain.feature.avatar.domain.model.OwnedAvatarCosmetic
import com.jaroapps.dailybrain.feature.avatar.domain.repository.AvatarRepository
import kotlinx.coroutines.flow.Flow

class ObserveOwnedAvatarCosmeticsUseCase(
    private val repository: AvatarRepository,
) {
    operator fun invoke(): Flow<Set<OwnedAvatarCosmetic>> = repository.observeOwnedCosmetics()
}

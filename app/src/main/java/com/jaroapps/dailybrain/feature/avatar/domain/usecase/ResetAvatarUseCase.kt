package com.jaroapps.dailybrain.feature.avatar.domain.usecase

import com.jaroapps.dailybrain.feature.avatar.domain.repository.AvatarRepository

class ResetAvatarUseCase(
    private val repository: AvatarRepository,
) {
    suspend operator fun invoke() = repository.resetAvatar()
}

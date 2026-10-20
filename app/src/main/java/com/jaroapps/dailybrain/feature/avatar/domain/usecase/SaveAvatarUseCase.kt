package com.jaroapps.dailybrain.feature.avatar.domain.usecase

import com.jaroapps.dailybrain.feature.avatar.domain.model.AvatarConfiguration
import com.jaroapps.dailybrain.feature.avatar.domain.repository.AvatarRepository

class SaveAvatarUseCase(
    private val repository: AvatarRepository,
) {
    suspend operator fun invoke(configuration: AvatarConfiguration) {
        require(configuration.schemaVersion == AvatarConfiguration.CURRENT_SCHEMA_VERSION) {
            "Unsupported avatar schema version: ${configuration.schemaVersion}"
        }
        require(configuration.skinToneId in 1..5) { "Unknown skin tone ID" }
        require(configuration.hairStyleId in 1..3) { "Unknown hair style ID" }
        require(configuration.hairColorId in 1..5) { "Unknown hair color ID" }
        require(configuration.eyeStyleId in 1..2) { "Unknown eye style ID" }
        require(configuration.mouthStyleId in 1..2) { "Unknown mouth style ID" }
        require(configuration.accessoryId in 0..2) { "Unknown accessory ID" }
        require(configuration.backgroundId in 1..4) { "Unknown background ID" }
        repository.saveAvatar(configuration)
    }
}

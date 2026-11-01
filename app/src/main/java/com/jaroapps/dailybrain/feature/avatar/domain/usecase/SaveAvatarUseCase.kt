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
        require(configuration.hairStyleId in 1..5) { "Unknown hair style ID" }
        require(configuration.hairColorId in 1..5) { "Unknown hair color ID" }
        require(configuration.eyeStyleId in 1..5) { "Unknown eye style ID" }
        require(configuration.mouthStyleId in 1..2) { "Unknown mouth style ID" }
        require(configuration.accessoryId in 0..2) { "Unknown accessory ID" }
        require(configuration.backgroundId in 1..10) { "Unknown background ID" }
        require(configuration.faceTypeId in 1..5) { "Unknown face type ID" }
        require(configuration.eyeColorId in 1..5) { "Unknown eye color ID" }
        require(configuration.clothingStyleId in 1..5) { "Unknown clothing style ID" }
        require(configuration.clothingColorId in 1..11) { "Unknown clothing color ID" }
        require(configuration.glassesId in 0..5) { "Unknown glasses ID" }
        require(configuration.frecklesId in 0..4) { "Unknown freckles ID" }
        require(configuration.bodyTypeId in 1..5) { "Unknown body type ID" }
        require(configuration.hairLengthId in 1..5) { "Unknown hair length ID" }
        require(configuration.facialHairId in 0..4) { "Unknown facial hair ID" }
        repository.saveAvatar(configuration)
    }
}

package com.jaroapps.dailybrain.feature.avatar.data

import com.jaroapps.dailybrain.feature.avatar.domain.model.AvatarConfiguration
import org.json.JSONObject

/** Keeps JSON parsing and backward-compatible defaults out of the domain layer. */
internal object AvatarJsonCodec {
    fun encode(value: AvatarConfiguration): String = JSONObject()
        .put("schemaVersion", AvatarConfiguration.CURRENT_SCHEMA_VERSION)
        .put("skinToneId", value.skinToneId)
        .put("hairStyleId", value.hairStyleId)
        .put("hairColorId", value.hairColorId)
        .put("eyeStyleId", value.eyeStyleId)
        .put("mouthStyleId", value.mouthStyleId)
        .put("accessoryId", value.accessoryId)
        .put("backgroundId", value.backgroundId)
        .toString()

    fun decode(json: String?): AvatarConfiguration {
        if (json.isNullOrBlank()) return AvatarConfiguration.Default
        return runCatching {
            val objectValue = JSONObject(json)
            val version = objectValue.optInt("schemaVersion", 1)
            if (version != AvatarConfiguration.CURRENT_SCHEMA_VERSION) {
                return AvatarConfiguration.Default
            }
            AvatarConfiguration(
                schemaVersion = version,
                skinToneId = objectValue.optInt("skinToneId", AvatarConfiguration.DEFAULT_SKIN_TONE_ID)
                    .takeIf { it in 1..5 } ?: AvatarConfiguration.DEFAULT_SKIN_TONE_ID,
                hairStyleId = objectValue.optInt("hairStyleId", AvatarConfiguration.DEFAULT_HAIR_STYLE_ID)
                    .takeIf { it in 1..3 } ?: AvatarConfiguration.DEFAULT_HAIR_STYLE_ID,
                hairColorId = objectValue.optInt("hairColorId", AvatarConfiguration.DEFAULT_HAIR_COLOR_ID)
                    .takeIf { it in 1..5 } ?: AvatarConfiguration.DEFAULT_HAIR_COLOR_ID,
                eyeStyleId = objectValue.optInt("eyeStyleId", AvatarConfiguration.DEFAULT_EYE_STYLE_ID)
                    .takeIf { it in 1..2 } ?: AvatarConfiguration.DEFAULT_EYE_STYLE_ID,
                mouthStyleId = objectValue.optInt("mouthStyleId", AvatarConfiguration.DEFAULT_MOUTH_STYLE_ID)
                    .takeIf { it in 1..2 } ?: AvatarConfiguration.DEFAULT_MOUTH_STYLE_ID,
                accessoryId = objectValue.optInt("accessoryId", AvatarConfiguration.NO_ACCESSORY_ID)
                    .takeIf { it in 0..2 } ?: AvatarConfiguration.NO_ACCESSORY_ID,
                backgroundId = objectValue.optInt("backgroundId", AvatarConfiguration.DEFAULT_BACKGROUND_ID)
                    .takeIf { it in 1..4 } ?: AvatarConfiguration.DEFAULT_BACKGROUND_ID,
            )
        }.getOrDefault(AvatarConfiguration.Default)
    }
}

package com.jaroapps.dailybrain.feature.avatar.data

import com.jaroapps.dailybrain.feature.avatar.domain.model.AvatarConfiguration
import org.json.JSONObject

/** Versioned local JSON codec. Version 1 configurations are upgraded with defaults for new slots. */
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
        .put("faceTypeId", value.faceTypeId)
        .put("eyeColorId", value.eyeColorId)
        .put("clothingStyleId", value.clothingStyleId)
        .put("clothingColorId", value.clothingColorId)
        .put("glassesId", value.glassesId)
        .put("frecklesId", value.frecklesId)
        .toString()

    fun decode(json: String?): AvatarConfiguration {
        if (json.isNullOrBlank()) return AvatarConfiguration.Default
        return runCatching {
            val data = JSONObject(json)
            val version = data.optInt("schemaVersion", 1)
            if (version !in 1..AvatarConfiguration.CURRENT_SCHEMA_VERSION) {
                return AvatarConfiguration.Default
            }
            AvatarConfiguration(
                schemaVersion = AvatarConfiguration.CURRENT_SCHEMA_VERSION,
                skinToneId = data.optInt("skinToneId", AvatarConfiguration.DEFAULT_SKIN_TONE_ID).takeIf { it in 1..5 } ?: AvatarConfiguration.DEFAULT_SKIN_TONE_ID,
                hairStyleId = data.optInt("hairStyleId", AvatarConfiguration.DEFAULT_HAIR_STYLE_ID).takeIf { it in 1..4 } ?: AvatarConfiguration.DEFAULT_HAIR_STYLE_ID,
                hairColorId = data.optInt("hairColorId", AvatarConfiguration.DEFAULT_HAIR_COLOR_ID).takeIf { it in 1..5 } ?: AvatarConfiguration.DEFAULT_HAIR_COLOR_ID,
                eyeStyleId = data.optInt("eyeStyleId", AvatarConfiguration.DEFAULT_EYE_STYLE_ID).takeIf { it in 1..3 } ?: AvatarConfiguration.DEFAULT_EYE_STYLE_ID,
                mouthStyleId = data.optInt("mouthStyleId", AvatarConfiguration.DEFAULT_MOUTH_STYLE_ID).takeIf { it in 1..2 } ?: AvatarConfiguration.DEFAULT_MOUTH_STYLE_ID,
                accessoryId = data.optInt("accessoryId", AvatarConfiguration.NO_ACCESSORY_ID).takeIf { it in 0..2 } ?: AvatarConfiguration.NO_ACCESSORY_ID,
                backgroundId = data.optInt("backgroundId", AvatarConfiguration.DEFAULT_BACKGROUND_ID).takeIf { it in 1..12 } ?: AvatarConfiguration.DEFAULT_BACKGROUND_ID,
                faceTypeId = data.optInt("faceTypeId", AvatarConfiguration.DEFAULT_FACE_TYPE_ID).takeIf { it in 1..2 } ?: AvatarConfiguration.DEFAULT_FACE_TYPE_ID,
                eyeColorId = data.optInt("eyeColorId", AvatarConfiguration.DEFAULT_EYE_COLOR_ID).takeIf { it in 1..3 } ?: AvatarConfiguration.DEFAULT_EYE_COLOR_ID,
                clothingStyleId = data.optInt("clothingStyleId", AvatarConfiguration.DEFAULT_CLOTHING_STYLE_ID).takeIf { it in 1..4 } ?: AvatarConfiguration.DEFAULT_CLOTHING_STYLE_ID,
                clothingColorId = data.optInt("clothingColorId", AvatarConfiguration.DEFAULT_CLOTHING_COLOR_ID).takeIf { it in 1..11 } ?: AvatarConfiguration.DEFAULT_CLOTHING_COLOR_ID,
                glassesId = data.optInt("glassesId", AvatarConfiguration.NO_GLASSES_ID).takeIf { it in 0..3 } ?: AvatarConfiguration.NO_GLASSES_ID,
                frecklesId = data.optInt("frecklesId", AvatarConfiguration.NO_FRECKLES_ID).takeIf { it in 0..2 } ?: AvatarConfiguration.NO_FRECKLES_ID,
            )
        }.getOrDefault(AvatarConfiguration.Default)
    }
}

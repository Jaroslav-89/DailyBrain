package com.jaroapps.dailybrain.feature.avatar.domain.model

/**
 * Stable local avatar selection. IDs refer to bundled resources and remain numeric
 * in JSON so catalog labels can change without changing persisted configurations.
 */
data class AvatarConfiguration(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val skinToneId: Int = DEFAULT_SKIN_TONE_ID,
    val hairStyleId: Int = DEFAULT_HAIR_STYLE_ID,
    val hairColorId: Int = DEFAULT_HAIR_COLOR_ID,
    val eyeStyleId: Int = DEFAULT_EYE_STYLE_ID,
    val mouthStyleId: Int = DEFAULT_MOUTH_STYLE_ID,
    val accessoryId: Int = NO_ACCESSORY_ID,
    val backgroundId: Int = DEFAULT_BACKGROUND_ID,
    val faceTypeId: Int = DEFAULT_FACE_TYPE_ID,
    val eyeColorId: Int = DEFAULT_EYE_COLOR_ID,
    val clothingStyleId: Int = DEFAULT_CLOTHING_STYLE_ID,
    val clothingColorId: Int = DEFAULT_CLOTHING_COLOR_ID,
    val glassesId: Int = NO_GLASSES_ID,
    val frecklesId: Int = NO_FRECKLES_ID,
    val bodyTypeId: Int = DEFAULT_BODY_TYPE_ID,
    val hairLengthId: Int = DEFAULT_HAIR_LENGTH_ID,
) {
    companion object {
        const val CURRENT_SCHEMA_VERSION = 3
        const val DEFAULT_SKIN_TONE_ID = 2
        const val DEFAULT_HAIR_STYLE_ID = 1
        const val DEFAULT_HAIR_COLOR_ID = 2
        const val DEFAULT_EYE_STYLE_ID = 2
        const val DEFAULT_MOUTH_STYLE_ID = 1
        const val NO_ACCESSORY_ID = 0
        const val DEFAULT_BACKGROUND_ID = 1
        const val DEFAULT_FACE_TYPE_ID = 1
        const val DEFAULT_EYE_COLOR_ID = 1
        const val DEFAULT_CLOTHING_STYLE_ID = 1
        const val DEFAULT_CLOTHING_COLOR_ID = 6
        const val NO_GLASSES_ID = 0
        const val NO_FRECKLES_ID = 0
        const val DEFAULT_BODY_TYPE_ID = 3
        const val DEFAULT_HAIR_LENGTH_ID = 2

        val Default = AvatarConfiguration()
    }
}

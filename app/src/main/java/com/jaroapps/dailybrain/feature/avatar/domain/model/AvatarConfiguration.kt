package com.jaroapps.dailybrain.feature.avatar.domain.model

/**
 * Stable, serializable selection of avatar parts. IDs refer to bundled local assets,
 * so avatar rendering works offline and does not depend on a remote catalog.
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
) {
    companion object {
        const val CURRENT_SCHEMA_VERSION = 1
        const val DEFAULT_SKIN_TONE_ID = 2
        const val DEFAULT_HAIR_STYLE_ID = 1
        const val DEFAULT_HAIR_COLOR_ID = 2
        const val DEFAULT_EYE_STYLE_ID = 1
        const val DEFAULT_MOUTH_STYLE_ID = 1
        const val NO_ACCESSORY_ID = 0
        const val DEFAULT_BACKGROUND_ID = 1

        val Default = AvatarConfiguration()
    }
}

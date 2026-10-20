package com.jaroapps.dailybrain.feature.avatar.presentation

import android.content.Context
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import androidx.core.content.ContextCompat
import com.jaroapps.dailybrain.R
import com.jaroapps.dailybrain.feature.avatar.domain.model.AvatarConfiguration

/**
 * Builds a fresh LayerDrawable for each configuration. Drawable instances are mutable,
 * so they are intentionally not shared between ImageViews or cached globally.
 */
internal object AvatarDrawableFactory {
    private val skinTones = intArrayOf(
        0xFFFFE0BD.toInt(),
        0xFFF1C27D.toInt(),
        0xFFE0AC69.toInt(),
        0xFFC68642.toInt(),
        0xFF8D5524.toInt(),
    )
    private val hairColors = intArrayOf(
        0xFF241713.toInt(),
        0xFF49342E.toInt(),
        0xFF9A623B.toInt(),
        0xFFD6A85F.toInt(),
        0xFFB83F35.toInt(),
    )

    fun create(context: Context, configuration: AvatarConfiguration): LayerDrawable {
        val background = when (configuration.backgroundId) {
            2 -> R.drawable.avatar_background_2
            3 -> R.drawable.avatar_background_3
            4 -> R.drawable.avatar_background_4
            else -> R.drawable.avatar_background_1
        }
        val hair = when (configuration.hairStyleId) {
            2 -> R.drawable.avatar_hair_2
            3 -> R.drawable.avatar_hair_3
            else -> R.drawable.avatar_hair_1
        }
        val eyes = if (configuration.eyeStyleId == 2) R.drawable.avatar_eyes_2 else R.drawable.avatar_eyes_1
        val mouth = if (configuration.mouthStyleId == 2) R.drawable.avatar_mouth_2 else R.drawable.avatar_mouth_1
        val accessory = when (configuration.accessoryId) {
            1 -> R.drawable.avatar_accessory_1
            2 -> R.drawable.avatar_accessory_2
            else -> R.drawable.avatar_empty
        }

        val layers = arrayOf(
            drawable(context, background),
            drawable(context, R.drawable.avatar_face).apply {
                setTint(skinTones[(configuration.skinToneId - 1).coerceIn(skinTones.indices)])
            },
            drawable(context, hair).apply {
                setTint(hairColors[(configuration.hairColorId - 1).coerceIn(hairColors.indices)])
            },
            drawable(context, eyes),
            drawable(context, mouth),
            drawable(context, accessory),
        )
        return LayerDrawable(layers).apply {
            for (index in layers.indices) setLayerInset(index, 0, 0, 0, 0)
        }
    }

    private fun drawable(context: Context, resourceId: Int): Drawable =
        requireNotNull(ContextCompat.getDrawable(context, resourceId)).mutate()
}

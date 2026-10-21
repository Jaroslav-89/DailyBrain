package com.jaroapps.dailybrain.feature.avatar.presentation

import android.content.Context
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import androidx.core.content.ContextCompat
import com.jaroapps.dailybrain.R
import com.jaroapps.dailybrain.feature.avatar.domain.model.AvatarConfiguration

/** Assembles fresh, aligned vector layers in a fixed z-order. */
internal object AvatarDrawableFactory {
    private val skinTones = intArrayOf(0xFFFFE0BD.toInt(), 0xFFF1C27D.toInt(), 0xFFE0AC69.toInt(), 0xFFC68642.toInt(), 0xFF8D5524.toInt())
    private val hairColors = intArrayOf(0xFF241713.toInt(), 0xFF49342E.toInt(), 0xFF9A623B.toInt(), 0xFFD6A85F.toInt(), 0xFFB83F35.toInt())
    private val eyeColors = intArrayOf(0xFF5B3822.toInt(), 0xFF3984C6.toInt(), 0xFF39835A.toInt(), 0xFF7D8790.toInt(), 0xFF9A7445.toInt())
    private val clothingColors = intArrayOf(
        0xFF171717.toInt(), 0xFFFFFFFF.toInt(), 0xFFE53935.toInt(),
        0xFF2E7D32.toInt(), 0xFFF9C80E.toInt(), 0xFF1565C0.toInt(),
        0xFF81D4FA.toInt(), 0xFFF4A6C1.toInt(), 0xFFD7B98E.toInt(),
        0xFFAED581.toInt(), 0xFFB39DDB.toInt(),
    )

    fun create(context: Context, configuration: AvatarConfiguration): LayerDrawable {
        val background = when (configuration.backgroundId) {
            2 -> R.drawable.avatar_background_2
            3 -> R.drawable.avatar_background_3
            4 -> R.drawable.avatar_background_4
            5 -> R.drawable.avatar_background_5
            6 -> R.drawable.avatar_background_6
            7 -> R.drawable.avatar_background_7
            8 -> R.drawable.avatar_background_8
            9 -> R.drawable.avatar_background_9
            10 -> R.drawable.avatar_background_10
            11 -> R.drawable.avatar_background_11
            12 -> R.drawable.avatar_background_12
            else -> R.drawable.avatar_background_1
        }
        val face = when (configuration.faceTypeId) {
            2 -> R.drawable.avatar_face_male
            3 -> R.drawable.avatar_face_round
            4 -> R.drawable.avatar_face_narrow
            5 -> R.drawable.avatar_face_wide
            else -> R.drawable.avatar_face_female
        }
        val hairLength = when (configuration.hairLengthId) {
            1 -> R.drawable.avatar_hair_1
            2 -> R.drawable.avatar_hair_2
            3 -> R.drawable.avatar_hair_6
            4 -> R.drawable.avatar_hair_7
            else -> R.drawable.avatar_hair_8
        }
        val hairStyle = when (configuration.hairStyleId) {
            2 -> R.drawable.avatar_hair_2
            3 -> R.drawable.avatar_hair_3
            4 -> R.drawable.avatar_hair_4
            5 -> R.drawable.avatar_hair_5
            else -> R.drawable.avatar_hair_1
        }
        val eyes = when (configuration.eyeStyleId) {
            2 -> R.drawable.avatar_eyes_1
            3 -> R.drawable.avatar_eyes_3
            4 -> R.drawable.avatar_eyes_4
            5 -> R.drawable.avatar_eyes_5
            else -> R.drawable.avatar_eyes_2
        }
        val clothing = when (configuration.clothingStyleId) {
            2 -> R.drawable.avatar_clothing_2
            3 -> R.drawable.avatar_clothing_3
            4 -> R.drawable.avatar_clothing_4
            5 -> R.drawable.avatar_clothing_5
            else -> R.drawable.avatar_clothing_1
        }
        val body = when (configuration.bodyTypeId) {
            1 -> R.drawable.avatar_body_1
            2 -> R.drawable.avatar_body_2
            4 -> R.drawable.avatar_body_4
            5 -> R.drawable.avatar_body_5
            else -> R.drawable.avatar_body_3
        }
        val glasses = when (configuration.glassesId) {
            1 -> R.drawable.avatar_glasses_1
            2 -> R.drawable.avatar_glasses_2
            3 -> R.drawable.avatar_glasses_3
            else -> R.drawable.avatar_empty
        }
        val freckles = when (configuration.frecklesId) {
            1 -> R.drawable.avatar_freckles_1
            2 -> R.drawable.avatar_freckles_2
            else -> R.drawable.avatar_empty
        }
        val accessory = when (configuration.accessoryId) {
            1 -> R.drawable.avatar_accessory_1
            2 -> R.drawable.avatar_accessory_2
            else -> R.drawable.avatar_empty
        }

        val skinTint = skinTones[(configuration.skinToneId - 1).coerceIn(skinTones.indices)]
        val hairTint = hairColors[(configuration.hairColorId - 1).coerceIn(hairColors.indices)]
        val layers = arrayOf(
            drawable(context, background),
            drawable(context, body).apply { setTint(skinTint) },
            drawable(context, clothing).apply {
                setTint(clothingColors[(configuration.clothingColorId - 1).coerceIn(clothingColors.indices)])
            },
            drawable(context, R.drawable.avatar_neck).apply { setTint(skinTint) },
            drawable(context, face).apply { setTint(skinTint) },
            drawable(context, hairLength).apply { setTint(hairTint) },
            drawable(context, hairStyle).apply { setTint(hairTint) },
            drawable(context, eyes).apply {
                setTint(eyeColors[(configuration.eyeColorId - 1).coerceIn(eyeColors.indices)])
            },
            drawable(context, if (configuration.mouthStyleId == 2) R.drawable.avatar_mouth_2 else R.drawable.avatar_mouth_1),
            drawable(context, freckles),
            drawable(context, glasses),
            drawable(context, accessory),
        )
        return LayerDrawable(layers).apply {
            for (index in layers.indices) setLayerInset(index, 0, 0, 0, 0)
        }
    }

    private fun drawable(context: Context, resourceId: Int): Drawable =
        requireNotNull(ContextCompat.getDrawable(context, resourceId)).mutate()
}

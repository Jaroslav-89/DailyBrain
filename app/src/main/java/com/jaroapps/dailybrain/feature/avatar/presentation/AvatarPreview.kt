package com.jaroapps.dailybrain.feature.avatar.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import com.jaroapps.dailybrain.R
import com.jaroapps.dailybrain.feature.avatar.domain.model.AvatarConfiguration

private val skinTones = listOf(
    Color(0xFFFFE0BD),
    Color(0xFFF1C27D),
    Color(0xFFE0AC69),
    Color(0xFFC68642),
    Color(0xFF8D5524),
)
private val hairColors = listOf(
    Color(0xFF241713),
    Color(0xFF49342E),
    Color(0xFF9A623B),
    Color(0xFFD6A85F),
    Color(0xFFB83F35),
)

@Composable
fun AvatarPreview(
    configuration: AvatarConfiguration,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.aspectRatio(4f / 5f)) {
        val background = when (configuration.backgroundId) {
            2 -> R.drawable.avatar_background_2
            3 -> R.drawable.avatar_background_3
            4 -> R.drawable.avatar_background_4
            else -> R.drawable.avatar_background_1
        }
        Image(
            painter = painterResource(background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds,
        )
        Image(
            painter = painterResource(R.drawable.avatar_face),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds,
            colorFilter = ColorFilter.tint(skinTones[(configuration.skinToneId - 1).coerceIn(skinTones.indices)]),
        )
        val hair = when (configuration.hairStyleId) {
            2 -> R.drawable.avatar_hair_2
            3 -> R.drawable.avatar_hair_3
            else -> R.drawable.avatar_hair_1
        }
        Image(
            painter = painterResource(hair),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds,
            colorFilter = ColorFilter.tint(hairColors[(configuration.hairColorId - 1).coerceIn(hairColors.indices)]),
        )
        Image(
            painter = painterResource(if (configuration.eyeStyleId == 2) R.drawable.avatar_eyes_2 else R.drawable.avatar_eyes_1),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds,
        )
        Image(
            painter = painterResource(if (configuration.mouthStyleId == 2) R.drawable.avatar_mouth_2 else R.drawable.avatar_mouth_1),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds,
        )
        when (configuration.accessoryId) {
            1 -> Image(
                painter = painterResource(R.drawable.avatar_accessory_1),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds,
            )
            2 -> Image(
                painter = painterResource(R.drawable.avatar_accessory_2),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds,
            )
        }
    }
}

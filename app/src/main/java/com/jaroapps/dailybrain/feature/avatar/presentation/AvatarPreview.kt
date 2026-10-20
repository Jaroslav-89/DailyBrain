package com.jaroapps.dailybrain.feature.avatar.presentation

import android.widget.ImageView
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.jaroapps.dailybrain.feature.avatar.domain.model.AvatarConfiguration

/** Shared preview used by both the editor and the profile card. */
@Composable
fun AvatarPreview(
    configuration: AvatarConfiguration,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        modifier = modifier.aspectRatio(4f / 5f),
        factory = { context ->
            ImageView(context).apply {
                scaleType = ImageView.ScaleType.FIT_CENTER
                contentDescription = "Аватар пользователя"
            }
        },
        update = { imageView ->
            imageView.setImageDrawable(AvatarDrawableFactory.create(imageView.context, configuration))
        },
    )
}

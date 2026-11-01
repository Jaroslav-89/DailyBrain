package com.jaroapps.dailybrain.feature.avatar.presentation

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.util.LruCache
import android.widget.ImageView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.caverock.androidsvg.SVG
import kotlin.math.max

/** Loads bundled avatar SVG assets and reuses their rasterized bitmaps. */
internal object AvatarAssetRenderer {
    private val bitmapCache = object : LruCache<String, Bitmap>(8 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / 1024
    }

    fun drawable(context: android.content.Context, assetPath: String, centerCrop: Boolean = false): Drawable {
        val bitmap = synchronized(bitmapCache) {
            bitmapCache.get(assetPath) ?: render(context, assetPath).also { bitmapCache.put(assetPath, it) }
        }
        return SvgBitmapDrawable(bitmap, centerCrop)
    }

    private fun render(context: android.content.Context, assetPath: String): Bitmap {
        val costume = assetPath.startsWith("avatar/costumes/")
        val width = if (costume) 256 else 512
        val height = if (costume) 320 else 512
        return runCatching {
            val svg = SVG.getFromAsset(context.assets, assetPath)
            val picture = svg.renderToPicture(width, height)
            Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { bitmap ->
                Canvas(bitmap).drawPicture(picture)
            }
        }.getOrElse {
            Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
                eraseColor(Color.TRANSPARENT)
            }
        }
    }
}

private class SvgBitmapDrawable(
    private val bitmap: Bitmap,
    private val centerCrop: Boolean,
) : Drawable() {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    override fun draw(canvas: Canvas) {
        val target = bounds
        if (target.isEmpty) return
        if (!centerCrop) {
            canvas.drawBitmap(bitmap, null, target, paint)
            return
        }
        val scale = max(
            target.width().toFloat() / bitmap.width,
            target.height().toFloat() / bitmap.height,
        )
        val width = bitmap.width * scale
        val height = bitmap.height * scale
        val destination = RectF(
            target.centerX() - width / 2f,
            target.centerY() - height / 2f,
            target.centerX() + width / 2f,
            target.centerY() + height / 2f,
        )
        val saveCount = canvas.save()
        canvas.clipRect(target)
        canvas.drawBitmap(bitmap, null, destination, paint)
        canvas.restoreToCount(saveCount)
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
        invalidateSelf()
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
        invalidateSelf()
    }

    @Deprecated("Deprecated in Android")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    override fun getIntrinsicWidth(): Int = bitmap.width
    override fun getIntrinsicHeight(): Int = bitmap.height
}

@Composable
fun AvatarAssetPreview(
    assetPath: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
    centerCrop: Boolean = false,
) {
    val context = LocalContext.current
    val drawable = remember(context, assetPath, centerCrop) {
        AvatarAssetRenderer.drawable(context, assetPath, centerCrop)
    }
    AndroidView(
        modifier = modifier,
        factory = { viewContext ->
            ImageView(viewContext).apply {
                scaleType = if (centerCrop) ImageView.ScaleType.CENTER_CROP else ImageView.ScaleType.FIT_CENTER
                this.contentDescription = contentDescription
            }
        },
        update = { imageView ->
            imageView.setImageDrawable(drawable)
            imageView.contentDescription = contentDescription
        },
    )
}

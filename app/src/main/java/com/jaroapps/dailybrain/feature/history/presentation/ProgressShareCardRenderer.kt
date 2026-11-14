package com.jaroapps.dailybrain.feature.history.presentation

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.LinearGradient
import android.graphics.Shader
import kotlin.math.min

enum class ShareCardTemplate { CLASSIC, PREMIUM }

/**
 * Creates a self-contained 4:5 share image. The artwork is drawn locally so sharing
 * works offline and does not depend on remote image hosts.
 */
object ProgressShareCardRenderer {
    private const val WIDTH = 1080
    private const val HEIGHT = 1350

    fun render(
        template: ShareCardTemplate,
        displayName: String,
        currentStreak: Int,
        bestStreak: Int,
        weeklyProgress: Int,
        monthlyProgress: Int,
        monthlyTarget: Int,
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val premium = template == ShareCardTemplate.PREMIUM
        val backgroundTop = if (premium) 0xFF071D19.toInt() else 0xFFF3FBF7.toInt()
        val backgroundBottom = if (premium) 0xFF123F34.toInt() else 0xFFDCEFE6.toInt()
        val accent = if (premium) 0xFFE4C47A.toInt() else 0xFF167B59.toInt()
        val primaryText = if (premium) 0xFFF8F3E8.toInt() else 0xFF123C30.toInt()
        val secondaryText = if (premium) 0xFFC9D8CE.toInt() else 0xFF527568.toInt()

        val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, 0f, WIDTH.toFloat(), HEIGHT.toFloat(), backgroundTop, backgroundBottom, Shader.TileMode.CLAMP)
        }
        canvas.drawRect(0f, 0f, WIDTH.toFloat(), HEIGHT.toFloat(), backgroundPaint)
        backgroundPaint.shader = null

        if (premium) drawPremiumArtwork(canvas, accent) else drawClassicArtwork(canvas, accent)

        val panelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (premium) 0xD90A2B24.toInt() else 0xDFFFFFFF.toInt()
        }
        canvas.drawRoundRect(RectF(66f, 210f, 1014f, 1240f), 44f, 44f, panelPaint)
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (premium) 0x66E4C47A.toInt() else 0x553A9B78.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        canvas.drawRoundRect(RectF(66f, 210f, 1014f, 1240f), 44f, 44f, borderPaint)

        drawCentered(canvas, "DAILYBRAIN", 100f, 38f, accent, true)
        drawCentered(canvas, "МОЯ СЕРИЯ", 310f, 38f, secondaryText, true)
        drawCentered(canvas, currentStreak.coerceAtLeast(0).toString(), 565f, 230f, primaryText, true)
        drawCentered(canvas, "ДНЕЙ ПОДРЯД", 640f, 40f, accent, true)
        drawCentered(canvas, "Мой прогресс в тренировке мозга", 710f, 30f, secondaryText, false)

        val tileTop = 790f
        val tileBottom = 1015f
        val tileWidth = 270f
        val gap = 24f
        val startX = (WIDTH - (tileWidth * 3 + gap * 2)) / 2f
        val tileFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (premium) 0xFF123B32.toInt() else 0xFFE8F5EE.toInt()
        }
        val tileStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (premium) 0x88E4C47A.toInt() else 0x553A9B78.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        val metrics = listOf(
            Triple("ЛУЧШАЯ СЕРИЯ", bestStreak.coerceAtLeast(0).toString(), "дней"),
            Triple("НЕДЕЛЯ", "${weeklyProgress.coerceIn(0, 7)}/7", "дней"),
            Triple("МЕСЯЦ", "${monthlyProgress.coerceAtLeast(0)}/${monthlyTarget.coerceAtLeast(1)}", "дней"),
        )
        metrics.forEachIndexed { index, metric ->
            val left = startX + index * (tileWidth + gap)
            val rect = RectF(left, tileTop, left + tileWidth, tileBottom)
            canvas.drawRoundRect(rect, 28f, 28f, tileFill)
            canvas.drawRoundRect(rect, 28f, 28f, tileStroke)
            drawCentered(canvas, metric.first, tileTop + 55f, 20f, secondaryText, true, left, left + tileWidth)
            drawCentered(canvas, metric.second, tileTop + 135f, 48f, primaryText, true, left, left + tileWidth)
            drawCentered(canvas, metric.third, tileTop + 180f, 22f, secondaryText, false, left, left + tileWidth)
        }

        val name = displayName.trim().ifBlank { "Участник DailyBrain" }.take(28)
        drawCentered(canvas, name, 1110f, 42f, primaryText, true)
        drawCentered(canvas, "Присоединяйтесь к Daily Brain", 1150f, 29f, secondaryText, true)
        drawCentered(canvas, "и выполняйте короткие упражнения", 1192f, 26f, secondaryText, false)
        drawCentered(canvas, "для развития интеллекта и памяти!", 1234f, 26f, secondaryText, false)
        drawCentered(canvas, "dailybrain", 1300f, 22f, accent, true)
        return bitmap
    }

    private fun drawClassicArtwork(canvas: Canvas, accent: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x223A9B78 }
        val path = Path().apply {
            moveTo(0f, 0f); lineTo(430f, 0f); cubicTo(350f, 170f, 250f, 150f, 0f, 300f); close()
        }
        canvas.drawPath(path, paint)
        paint.color = 0x1F79B99A
        val lower = Path().apply {
            moveTo(0f, 1080f); cubicTo(250f, 980f, 520f, 1170f, 1080f, 990f); lineTo(1080f, 1350f); lineTo(0f, 1350f); close()
        }
        canvas.drawPath(lower, paint)
        paint.color = accent
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 5f
        canvas.drawCircle(900f, 125f, 54f, paint)
        canvas.drawCircle(900f, 125f, 78f, paint)
    }

    private fun drawPremiumArtwork(canvas: Canvas, accent: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accent
            style = Paint.Style.STROKE
            strokeWidth = 5f
            alpha = 175
        }
        val path = Path().apply {
            moveTo(-100f, 100f); cubicTo(240f, 260f, 620f, -10f, 1180f, 230f)
        }
        canvas.drawPath(path, paint)
        val second = Path().apply {
            moveTo(-80f, 1120f); cubicTo(300f, 920f, 720f, 1320f, 1160f, 1080f)
        }
        canvas.drawPath(second, paint)
        paint.strokeWidth = 2f
        for (i in 0..11) {
            val x = 90f + i * 82f
            canvas.drawCircle(x, 125f + (i % 3) * 20f, 4f, paint)
        }
    }

    private fun drawCentered(
        canvas: Canvas,
        text: String,
        baseline: Float,
        size: Float,
        color: Int,
        bold: Boolean,
        left: Float = 0f,
        right: Float = WIDTH.toFloat(),
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            textSize = size
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create("sans-serif", if (bold) Typeface.BOLD else Typeface.NORMAL)
        }
        val available = right - left - 28f
        var value = text
        while (paint.measureText(value) > available && value.length > 4) {
            value = value.dropLast(1)
        }
        canvas.drawText(value, (left + right) / 2f, baseline, paint)
    }
}

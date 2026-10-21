package com.jaroapps.dailybrain.feature.avatar.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme

internal enum class AvatarEditorTab(val label: String) {
    SKIN("Цвет кожи"),
    FACE("Форма лица"),
    BODY("Тип тела"),
    EYES("Глаза"),
    HAIRSTYLE("Тип причёски"),
    HAIR_LENGTH("Длина волос"),
    HAIR_COLOR("Цвет волос"),
    CLOTHING("Тип одежды"),
    CLOTHING_COLOR("Цвет одежды"),
    OTHER("Дополнительные настройки"),
    GLASSES("Очки"),
    FRECKLES("Веснушки"),
    FACIAL_HAIR("Борода и усы"),
    BACKGROUND("Градиент фона"),
}

@Composable
internal fun AvatarTabIcon(tab: AvatarEditorTab, selected: Boolean) {
    val accent = MaterialTheme.colorScheme.primary
    val outline = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(
        Modifier.size(24.dp).semantics { contentDescription = tab.label },
    ) {
        val unit = size.minDimension / 24f
        fun p(x: Float, y: Float) = Offset(x * unit, y * unit)
        fun line(x1: Float, y1: Float, x2: Float, y2: Float, width: Float = 2f) {
            drawLine(outline, p(x1, y1), p(x2, y2), strokeWidth = width * unit, cap = StrokeCap.Round)
        }
        fun ring(x: Float, y: Float, radius: Float, width: Float = 2f) {
            drawCircle(outline, radius * unit, p(x, y), style = Stroke(width * unit))
        }
        when (tab) {
            AvatarEditorTab.SKIN -> {
                drawCircle(Color(0xFFFFE0BD), 4f * unit, p(6f, 12f))
                drawCircle(Color(0xFFE0AC69), 4f * unit, p(12f, 12f))
                drawCircle(Color(0xFF8D5524), 4f * unit, p(18f, 12f))
            }
            AvatarEditorTab.FACE -> {
                drawOval(outline, p(5f, 2f), Size(14f * unit, 19f * unit), style = Stroke(2f * unit))
                drawCircle(outline, 1f * unit, p(9f, 10f))
                drawCircle(outline, 1f * unit, p(15f, 10f))
                line(10f, 16f, 14f, 16f, 1.5f)
            }
            AvatarEditorTab.BODY -> {
                ring(12f, 5f, 3f)
                line(12f, 8.5f, 12f, 15f)
                line(12f, 10f, 5f, 15f)
                line(12f, 10f, 19f, 15f)
                line(12f, 15f, 7f, 22f)
                line(12f, 15f, 17f, 22f)
            }
            AvatarEditorTab.EYES -> {
                drawOval(outline, p(2f, 7f), Size(9f * unit, 8f * unit), style = Stroke(2f * unit))
                drawOval(outline, p(13f, 7f), Size(9f * unit, 8f * unit), style = Stroke(2f * unit))
                drawCircle(accent, 1.8f * unit, p(6.5f, 11f))
                drawCircle(accent, 1.8f * unit, p(17.5f, 11f))
            }
            AvatarEditorTab.HAIRSTYLE -> {
                drawOval(outline, p(5f, 7f), Size(14f * unit, 15f * unit), style = Stroke(1.5f * unit))
                val path = Path().apply {
                    moveTo(4f * unit, 12f * unit); cubicTo(2f * unit, 4f * unit, 7f * unit, 1f * unit, 12f * unit, 2f * unit)
                    cubicTo(18f * unit, 0f, 23f * unit, 5f * unit, 20f * unit, 12f * unit)
                    lineTo(17f * unit, 8f * unit); lineTo(12f * unit, 9f * unit); lineTo(7f * unit, 7f * unit); close()
                }
                drawPath(path, outline)
            }
            AvatarEditorTab.HAIR_LENGTH -> {
                line(7f, 4f, 7f, 20f, 2.5f)
                line(17f, 4f, 17f, 20f, 2.5f)
                line(7f, 4f, 12f, 2f)
                line(17f, 4f, 12f, 2f)
                line(7f, 20f, 12f, 22f)
                line(17f, 20f, 12f, 22f)
            }
            AvatarEditorTab.HAIR_COLOR -> {
                drawCircle(Color(0xFF241713), 4f * unit, p(6f, 12f))
                drawCircle(Color(0xFF9A623B), 4f * unit, p(12f, 12f))
                drawCircle(Color(0xFFD6A85F), 4f * unit, p(18f, 12f))
            }
            AvatarEditorTab.CLOTHING -> {
                val path = Path().apply {
                    moveTo(8f * unit, 4f * unit); lineTo(3f * unit, 7f * unit); lineTo(1f * unit, 13f * unit); lineTo(6f * unit, 15f * unit)
                    lineTo(8f * unit, 11f * unit); lineTo(8f * unit, 22f * unit); lineTo(16f * unit, 22f * unit); lineTo(16f * unit, 11f * unit)
                    lineTo(18f * unit, 15f * unit); lineTo(23f * unit, 13f * unit); lineTo(21f * unit, 7f * unit); lineTo(16f * unit, 4f * unit)
                    cubicTo(14f * unit, 8f * unit, 10f * unit, 8f * unit, 8f * unit, 4f * unit); close()
                }
                drawPath(path, outline, style = Stroke(2f * unit))
            }
            AvatarEditorTab.CLOTHING_COLOR -> {
                val colors = listOf(Color(0xFFE53935), Color(0xFF1565C0), Color(0xFF2E7D32), Color(0xFFF9C80E))
                colors.forEachIndexed { index, color ->
                    val x = if (index % 2 == 0) 3f else 13f
                    val y = if (index < 2) 3f else 13f
                    drawRoundRect(color, p(x, y), Size(8f * unit, 8f * unit), androidx.compose.ui.geometry.CornerRadius(2f * unit))
                }
            }
            AvatarEditorTab.OTHER -> {
                line(12f, 2f, 12f, 22f)
                line(2f, 12f, 22f, 12f)
                line(5f, 5f, 19f, 19f)
                line(19f, 5f, 5f, 19f)
            }
            AvatarEditorTab.GLASSES -> {
                drawRoundRect(outline, p(2f, 7f), Size(8f * unit, 8f * unit), androidx.compose.ui.geometry.CornerRadius(2f * unit), style = Stroke(2f * unit))
                drawRoundRect(outline, p(14f, 7f), Size(8f * unit, 8f * unit), androidx.compose.ui.geometry.CornerRadius(2f * unit), style = Stroke(2f * unit))
                line(10f, 10f, 14f, 10f, 2f)
            }
            AvatarEditorTab.FRECKLES -> {
                drawOval(outline, p(4f, 2f), Size(16f * unit, 20f * unit), style = Stroke(1.5f * unit))
                listOf(p(8f, 12f), p(10f, 14f), p(8f, 16f), p(16f, 12f), p(14f, 14f), p(16f, 16f)).forEach {
                    drawCircle(accent, 0.9f * unit, it)
                }
            }
            AvatarEditorTab.FACIAL_HAIR -> {
                val moustache = Path().apply {
                    moveTo(3f * unit, 10f * unit); quadraticTo(7f * unit, 8f * unit, 11f * unit, 11f * unit)
                    quadraticTo(12f * unit, 12f * unit, 13f * unit, 11f * unit); quadraticTo(18f * unit, 8f * unit, 21f * unit, 10f * unit)
                    quadraticTo(19f * unit, 16f * unit, 14f * unit, 14f * unit); lineTo(12f * unit, 13f * unit); lineTo(10f * unit, 14f * unit)
                    quadraticTo(5f * unit, 16f * unit, 3f * unit, 10f * unit); close()
                }
                drawPath(moustache, outline)
                line(6f, 17f, 8f, 21f, 1.5f)
                line(18f, 17f, 16f, 21f, 1.5f)
            }
            AvatarEditorTab.BACKGROUND -> {
                drawRoundRect(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFF80DEEA), Color(0xFF7E57C2)),
                        start = Offset(0f, 0f),
                        end = Offset(size.width, size.height),
                    ),
                    topLeft = p(2f, 2f),
                    size = Size(20f * unit, 20f * unit),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f * unit),
                )
                line(5f, 5f, 19f, 19f, 1.5f)
            }
        }
    }
}

package com.jaroapps.dailybrain.feature.profile.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter

@Composable
fun InviteQrCode(payload: String, modifier: Modifier = Modifier) {
    val matrix = remember(payload) {
        runCatching {
            QRCodeWriter().encode(
                payload,
                BarcodeFormat.QR_CODE,
                480,
                480,
                mapOf(EncodeHintType.MARGIN to 1),
            )
        }.getOrNull()
    }
    Box(
        modifier = modifier.aspectRatio(1f).background(Color.White),
        contentAlignment = Alignment.Center,
    ) {
        if (matrix != null) {
            Canvas(Modifier.fillMaxSize()) {
                val cellWidth = size.width / matrix.width
                val cellHeight = size.height / matrix.height
                drawRect(Color.White)
                for (x in 0 until matrix.width) {
                    for (y in 0 until matrix.height) {
                        if (matrix[x, y]) {
                            drawRect(
                                color = Color(0xFF111111),
                                topLeft = Offset(x * cellWidth, y * cellHeight),
                                size = Size(cellWidth, cellHeight),
                            )
                        }
                    }
                }
            }
        }
    }
}

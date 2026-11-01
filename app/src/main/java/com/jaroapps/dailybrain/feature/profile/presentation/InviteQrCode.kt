package com.jaroapps.dailybrain.feature.profile.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.sp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun InviteQrCode(payload: String, modifier: Modifier = Modifier) {
    var matrix by remember(payload) { mutableStateOf<BitMatrix?>(null) }
    var generationFailed by remember(payload) { mutableStateOf(false) }

    // QR encoding is CPU-bound. Keep it off the main thread so opening the dialog
    // does not block the first frame, especially on devices where ZXing startup is slower.
    LaunchedEffect(payload) {
        matrix = null
        generationFailed = false
        try {
            matrix = withContext(Dispatchers.Default) {
                // Zero requested dimensions preserve the compact QR module matrix.
                // The previous 480x480 output was needlessly expanded before drawing.
                QRCodeWriter().encode(
                    payload,
                    BarcodeFormat.QR_CODE,
                    0,
                    0,
                    mapOf(EncodeHintType.MARGIN to 4),
                )
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            generationFailed = true
        }
    }

    val currentMatrix = matrix
    val qrPath = remember(currentMatrix) {
        currentMatrix?.let { encoded ->
            Path().apply {
                for (y in 0 until encoded.height) {
                    for (x in 0 until encoded.width) {
                        if (encoded[x, y]) {
                            addRect(
                                Rect(
                                    left = x.toFloat(),
                                    top = y.toFloat(),
                                    right = x + 1f,
                                    bottom = y + 1f,
                                ),
                            )
                        }
                    }
                }
            }
        }
    }

    Box(
        modifier = modifier.aspectRatio(1f).background(Color.White),
        contentAlignment = Alignment.Center,
    ) {
        when {
            currentMatrix != null && qrPath != null -> Canvas(Modifier.fillMaxSize()) {
                drawRect(Color.White)
                withTransform({
                    scale(
                        scaleX = size.width / currentMatrix.width.toFloat(),
                        scaleY = size.height / currentMatrix.height.toFloat(),
                        pivot = Offset.Zero,
                    )
                }) {
                    drawPath(qrPath, Color(0xFF111111))
                }
            }
            generationFailed -> Text(
                "Не удалось создать QR-код.",
                color = Color(0xFF111111),
                fontSize = 12.sp,
            )
            else -> CircularProgressIndicator()
        }
    }
}

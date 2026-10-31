package com.jaroapps.dailybrain.feature.profile.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import com.jaroapps.dailybrain.feature.profile.domain.model.UserProfile

@Composable
fun MyQrCodeDialog(
    profile: UserProfile?,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Мой профиль") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = profile?.effectiveDisplayName ?: "Профиль",
                    style = MaterialTheme.typography.titleMedium,
                )
                profile?.let {
                    Text(
                        text = "@${it.generatedAlias}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    listOfNotNull(it.city, it.country)
                        .filter(String::isNotBlank)
                        .joinToString(", ")
                        .takeIf(String::isNotBlank)
                        ?.let { location ->
                            Text(location, style = MaterialTheme.typography.bodySmall)
                        }
                }
                Spacer(Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    modifier = Modifier.size(216.dp),
                ) {
                    PlaceholderQrCode(
                        seed = profile?.generatedAlias?.hashCode() ?: 37,
                        modifier = Modifier.padding(12.dp),
                    )
                }
                Text(
                    text = "QR-код для обмена профилями появится позже.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Закрыть")
            }
        },
    )
}

@Composable
private fun PlaceholderQrCode(
    seed: Int,
    modifier: Modifier = Modifier,
) {
    val moduleCount = 29
    val matrix = remember(seed) {
        Array(moduleCount) { row ->
            BooleanArray(moduleCount) { col ->
                (row * 31 + col * 17 + seed).mod(7) < 3
            }
        }
    }

    Canvas(modifier = modifier.background(Color.White)) {
        val cellSize = size.width / moduleCount
        for (row in 0 until moduleCount) {
            for (col in 0 until moduleCount) {
                val value = qrModule(row, col, matrix)
                if (value) {
                    drawRect(
                        color = Color(0xFF161616),
                        topLeft = Offset(col * cellSize, row * cellSize),
                        size = Size(cellSize, cellSize),
                    )
                }
            }
        }
    }
}

private fun qrModule(row: Int, col: Int, matrix: Array<BooleanArray>): Boolean {
    val finderOrigins = listOf(0 to 0, 0 to 22, 22 to 0)
    for ((originRow, originCol) in finderOrigins) {
        if (row in (originRow - 1)..(originRow + 7) &&
            col in (originCol - 1)..(originCol + 7)
        ) {
            if (row !in originRow..(originRow + 6) || col !in originCol..(originCol + 6)) {
                return false
            }
            val y = row - originRow
            val x = col - originCol
            return x == 0 || x == 6 || y == 0 || y == 6 || (x in 2..4 && y in 2..4)
        }
    }
    return matrix[row][col]
}

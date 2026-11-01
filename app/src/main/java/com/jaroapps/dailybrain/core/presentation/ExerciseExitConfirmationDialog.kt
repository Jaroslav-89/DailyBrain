package com.jaroapps.dailybrain.core.presentation

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

@Composable
fun ExerciseExitConfirmationDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    onConfirmExit: () -> Unit,
) {
    if (!visible) return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Выйти из упражнения?") },
        text = {
            Text("При выходе до завершения результаты не будут засчитаны. Вы действительно хотите выйти?")
        },
        confirmButton = {
            TextButton(onClick = onConfirmExit) { Text("Выйти") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Продолжить") }
        },
    )
}

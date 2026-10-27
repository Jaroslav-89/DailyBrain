package com.jaroapps.dailybrain.feature.profile.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AddFriendsDialog(onDismiss: () -> Unit) {
    var selectedMethod by remember { mutableStateOf<String?>(null) }

    if (selectedMethod == null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Добавить друзей") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { selectedMethod = "по QR-коду" },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("По QR-коду") }
                    OutlinedButton(
                        onClick = { selectedMethod = "по коду приглашения" },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("По коду приглашения") }
                    OutlinedButton(
                        onClick = { selectedMethod = "по уникальному нику" },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("По уникальному нику") }
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) { Text("Закрыть") }
            },
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Добавить друзей") },
            text = { Text("Добавление друзей ${selectedMethod} пока не подключено.") },
            confirmButton = {
                TextButton(onClick = onDismiss) { Text("Понятно") }
            },
        )
    }
}

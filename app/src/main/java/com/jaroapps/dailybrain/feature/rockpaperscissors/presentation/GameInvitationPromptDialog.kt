package com.jaroapps.dailybrain.feature.rockpaperscissors.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.jaroapps.dailybrain.feature.rockpaperscissors.domain.model.GameRoomInvitation

@Composable
fun GameInvitationPromptDialog(
    invitation: GameRoomInvitation,
    isAccepting: Boolean,
    errorMessage: String?,
    onAccept: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Вас пригласили в игру!") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(invitation.inviterName + " приглашает вас в комнату «" + invitation.roomName + "».")
                Text(
                    "Примите приглашение, чтобы присоединиться к игре «Камень, ножницы, бумага».",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (isAccepting) CircularProgressIndicator()
            }
        },
        confirmButton = {
            TextButton(enabled = !isAccepting, onClick = onAccept) { Text("Принять") }
        },
        dismissButton = {
            TextButton(enabled = !isAccepting, onClick = onDismiss) { Text("Позже") }
        },
    )
}

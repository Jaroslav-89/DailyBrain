package com.jaroapps.dailybrain.feature.profile.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
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
fun AddFriendsDialog(
    googleLinked: Boolean,
    googleEmail: String?,
    isGoogleSignInInProgress: Boolean,
    googleLinkError: GoogleLinkError?,
    googleSignInError: String?,
    onContinueWithGoogle: () -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedMethod by remember { mutableStateOf<String?>(null) }

    if (!googleLinked) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Добавить друзей") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Чтобы добавлять друзей, сначала привяжите Google-аккаунт к этому локальному профилю.")
                    googleSignInError?.let {
                        Text(it, color = androidx.compose.material3.MaterialTheme.colorScheme.error)
                    }
                    when (googleLinkError) {
                        GoogleLinkError.ALREADY_LINKED_TO_ANOTHER_PROFILE -> Text(
                            "Этот Google-аккаунт уже привязан к другому профилю на этом устройстве.",
                            color = androidx.compose.material3.MaterialTheme.colorScheme.error,
                        )
                        GoogleLinkError.FIREBASE_NOT_CONFIGURED -> Text(
                            "Firebase не настроен. Добавьте app/google-services.json и включите Google в Firebase Authentication.",
                            color = androidx.compose.material3.MaterialTheme.colorScheme.error,
                        )
                        GoogleLinkError.FIREBASE_AUTH_FAILED -> Text(
                            "Firebase не подтвердил вход через Google. Проверьте настройки Google provider и повторите попытку.",
                            color = androidx.compose.material3.MaterialTheme.colorScheme.error,
                        )
                        GoogleLinkError.SAVE_FAILED -> Text(
                            "Не удалось сохранить привязку. Попробуйте ещё раз.",
                            color = androidx.compose.material3.MaterialTheme.colorScheme.error,
                        )
                        null -> Unit
                    }
                    OutlinedButton(
                        onClick = onContinueWithGoogle,
                        enabled = !isGoogleSignInInProgress,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (isGoogleSignInInProgress) {
                            CircularProgressIndicator()
                        } else {
                            Text("G")
                            Text("  Продолжить с Google")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) { Text("Закрыть") }
            },
        )
    } else if (selectedMethod == null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Добавить друзей") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Google: ${googleEmail ?: "аккаунт привязан"}")
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

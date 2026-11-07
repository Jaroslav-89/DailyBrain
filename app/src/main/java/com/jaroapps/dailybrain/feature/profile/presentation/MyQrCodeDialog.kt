package com.jaroapps.dailybrain.feature.profile.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.colorResource
import com.jaroapps.dailybrain.R
import com.jaroapps.dailybrain.feature.profile.domain.model.InviteCodePolicy
import com.jaroapps.dailybrain.feature.profile.domain.model.UserProfile

@Composable
fun MyQrCodeDialog(
    profile: UserProfile?,
    inviteCode: String?,
    isLoading: Boolean,
    error: String?,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Мой QR-код") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(profile?.effectiveDisplayName ?: "Профиль", style = MaterialTheme.typography.titleMedium)
                profile?.username?.takeIf(String::isNotBlank)?.let {
                    Text("@$it", color = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = colorResource(R.color.color_qr_background),
                    modifier = Modifier.size(216.dp).padding(4.dp),
                ) {
                    when {
                        inviteCode != null -> InviteQrCode(
                            payload = InviteCodePolicy.qrPayload(inviteCode),
                            modifier = Modifier.padding(8.dp),
                        )
                        isLoading -> Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            CircularProgressIndicator()
                        }
                        else -> Text(
                            error ?: "Для создания QR-кода подключите Google-аккаунт.",
                            modifier = Modifier.padding(12.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                inviteCode?.let {
                    Text("Код приглашения: $it", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Отсканируйте этот QR-код, чтобы найти ваш профиль.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (inviteCode == null && error != null) {
                    Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Закрыть") } },
    )
}

package com.jaroapps.dailybrain.feature.publicprofile.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jaroapps.dailybrain.feature.avatar.data.AvatarJsonCodec
import com.jaroapps.dailybrain.feature.avatar.presentation.AvatarPreview
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublicUserConnectionsScreen(
    profileFirebaseUid: String,
    connectionType: String,
    onBack: () -> Unit,
    onOpenUser: (String) -> Unit,
    viewModel: PublicUserConnectionsViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(profileFirebaseUid, connectionType) {
        viewModel.load(profileFirebaseUid, connectionType)
    }
    val title = if (connectionType == PublicUserConnectionsViewModel.FOLLOWING) "Подписки" else "Подписчики"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.Close, contentDescription = "Назад")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            when {
                state.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                state.error != null -> Text(state.error.orEmpty(), color = MaterialTheme.colorScheme.error)
                state.users.isEmpty() -> Text(
                    if (connectionType == PublicUserConnectionsViewModel.FOLLOWING) {
                        "Пользователь пока ни на кого не подписан."
                    } else {
                        "У пользователя пока нет подписчиков."
                    },
                    style = MaterialTheme.typography.bodyLarge,
                )
                else -> state.users.forEach { user ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { onOpenUser(user.firebaseUid) },
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            AvatarPreview(
                                configuration = AvatarJsonCodec.decode(user.avatarConfigurationJson),
                                modifier = Modifier.size(width = 48.dp, height = 60.dp),
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(
                                    user.displayName?.takeIf(String::isNotBlank)
                                        ?: user.username.takeIf(String::isNotBlank)?.let { "@$it" }
                                        ?: "Пользователь DailyBrain",
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                if (user.username.isNotBlank()) {
                                    Text("@${user.username}", color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

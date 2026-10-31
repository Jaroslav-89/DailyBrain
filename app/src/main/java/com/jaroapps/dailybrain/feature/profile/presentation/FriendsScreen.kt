package com.jaroapps.dailybrain.feature.profile.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jaroapps.dailybrain.feature.profile.data.auth.GoogleSignInClient
import com.jaroapps.dailybrain.feature.profile.data.auth.GoogleSignInConfigurationException
import org.koin.androidx.compose.koinInject
import org.koin.androidx.compose.koinViewModel
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.jaroapps.dailybrain.R
import com.jaroapps.dailybrain.feature.profile.domain.model.UsernamePolicy

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(
    onBack: () -> Unit,
    viewModel: ProfileViewModel = koinViewModel(),
    directoryViewModel: UserDirectoryViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val directoryState by directoryViewModel.uiState.collectAsStateWithLifecycle()
    val googleSignInClient: GoogleSignInClient = koinInject()
    val coroutineScope = rememberCoroutineScope()
    var isGoogleSignInInProgress by remember { mutableStateOf(false) }
    var googleSignInError by remember { mutableStateOf<String?>(null) }
    var selectedTab by remember { mutableIntStateOf(2) }
    var showAddFriends by remember { mutableStateOf(false) }
    var usernameQuery by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Друзья") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.action_close))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Подписки 0") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Подписчики 0") })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Поиск") })
            }
            Spacer(Modifier.height(24.dp))

            when (selectedTab) {
                0, 1 -> {
                    Text(
                        text = if (selectedTab == 0) "Вы пока ни на кого не подписаны" else "У вас пока нет подписчиков",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Spacer(Modifier.height(20.dp))
                    Button(onClick = { selectedTab = 2 }, modifier = Modifier.fillMaxWidth()) {
                        Text("Найти пользователя")
                    }
                }
                else -> {
                    if (directoryState.profile?.firebaseUid == null) {
                        Text(
                            "Чтобы искать пользователей по уникальному нику, сначала подключите Google-аккаунт.",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { showAddFriends = true }, modifier = Modifier.fillMaxWidth()) {
                            Text("Подключить Google")
                        }
                    } else {
                        Text(
                            "Найдите пользователя по точному нику DailyBrain.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = usernameQuery,
                            onValueChange = {
                                usernameQuery = UsernamePolicy.sanitizeInput(it)
                                directoryViewModel.resetSearch()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Ник пользователя") },
                            leadingIcon = { Text("@") },
                            singleLine = true,
                        )
                        Spacer(Modifier.height(10.dp))
                        Button(
                            onClick = { directoryViewModel.searchUserByUsername(usernameQuery) },
                            enabled = !directoryState.isSearching && UsernamePolicy.isValid(usernameQuery),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(if (directoryState.isSearching) "Ищем..." else "Найти")
                        }
                        directoryState.searchError?.let { error ->
                            Spacer(Modifier.height(12.dp))
                            Text(error, color = MaterialTheme.colorScheme.error)
                        }
                        if (directoryState.searchHasRun && directoryState.searchResult == null) {
                            Spacer(Modifier.height(16.dp))
                            Text("Пользователь с таким ником не найден.")
                        }
                        directoryState.searchResult?.let { user ->
                            Spacer(Modifier.height(16.dp))
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Text(
                                        user.displayName?.takeIf(String::isNotBlank) ?: ("@" + user.username),
                                        style = MaterialTheme.typography.titleMedium,
                                    )
                                    Text(
                                        "@" + user.username,
                                        color = MaterialTheme.colorScheme.primary,
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                    Text(
                                        "Пользователь DailyBrain",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddFriends) {
        AddFriendsDialog(
            googleLinked = state.profile?.firebaseUid != null,
            googleEmail = state.profile?.googleEmail,
            isGoogleSignInInProgress = isGoogleSignInInProgress || state.isGoogleLinking,
            googleLinkError = state.googleLinkError,
            googleLinkDiagnostic = state.googleLinkDiagnostic,
            googleSignInError = googleSignInError,
            onContinueWithGoogle = {
                coroutineScope.launch {
                    isGoogleSignInInProgress = true
                    googleSignInError = null
                    viewModel.clearGoogleLinkError()
                    try {
                        val signInResult = googleSignInClient.signIn()
                        viewModel.linkGoogleAccount(signInResult)
                    } catch (error: Exception) {
                        googleSignInError = if (error is GoogleSignInConfigurationException) {
                            error.message
                        } else {
                            "Не удалось выполнить вход с Google. Попробуйте ещё раз."
                        }
                    } finally {
                        isGoogleSignInInProgress = false
                    }
                }
            },
            onDismiss = {
                showAddFriends = false
                googleSignInError = null
                viewModel.clearGoogleLinkError()
            },
        )
    }
}

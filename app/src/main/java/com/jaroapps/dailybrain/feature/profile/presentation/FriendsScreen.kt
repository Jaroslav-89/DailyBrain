package com.jaroapps.dailybrain.feature.profile.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jaroapps.dailybrain.R
import com.jaroapps.dailybrain.feature.profile.domain.model.PublicUserProfile
import org.koin.androidx.compose.koinViewModel

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(
    onBack: () -> Unit,
    onOpenUserProfile: (String) -> Unit,
    viewModel: ProfileViewModel = koinViewModel(),
    directoryViewModel: UserDirectoryViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val directoryState by directoryViewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddFriendsDialog by remember { mutableStateOf(false) }
    var pendingUnfollowUser by remember { mutableStateOf<PublicUserProfile?>(null) }
    val firebaseUid = directoryState.profile?.firebaseUid
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, firebaseUid) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && firebaseUid != null) {
                directoryViewModel.refreshRelationships()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(firebaseUid) {
        if (firebaseUid != null) directoryViewModel.loadInviteCode()
    }

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
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Подписки ${directoryState.followingUsers.size}") },
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Подписчики ${directoryState.followerUsers.size}") },
                )
            }
            Spacer(Modifier.height(20.dp))
            if (selectedTab == 0) {
                Button(
                    onClick = { showAddFriendsDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Добавить друзей") }
                Spacer(Modifier.height(16.dp))
            }
            if (directoryState.isLoadingRelationships) {
                CircularProgressIndicator()
            } else if (directoryState.relationshipError != null) {
                Text(directoryState.relationshipError.orEmpty(), color = MaterialTheme.colorScheme.error)
            } else {
                val users = if (selectedTab == 0) directoryState.followingUsers else directoryState.followerUsers
                if (users.isEmpty()) {
                    Text(
                        if (selectedTab == 0) "Вы пока ни на кого не подписаны."
                        else "У вас пока нет подписчиков.",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                } else {
                    users.forEach { user ->
                        FriendUserCard(
                            user = user,
                            showUnfollow = selectedTab == 0,
                            onUnfollow = { pendingUnfollowUser = user },
                            onOpenProfile = { onOpenUserProfile(user.firebaseUid) },
                            showFollowBack = selectedTab == 1 &&
                                directoryState.followingUsers.none { it.firebaseUid == user.firebaseUid },
                            onFollowBack = { directoryViewModel.followUser(user) },
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }

    pendingUnfollowUser?.let { user ->
        AlertDialog(
            onDismissRequest = { pendingUnfollowUser = null },
            title = { Text("Отписаться от пользователя?") },
            text = {
                Text(
                    "Вы действительно хотите отписаться от " +
                        (user.username.takeIf(String::isNotBlank)?.let { "@$it" } ?: "этого пользователя") +
                        "? Его достижения больше не будут показываться в вашей ленте.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    directoryViewModel.unfollowUser(user)
                    pendingUnfollowUser = null
                }) { Text("Отписаться", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { pendingUnfollowUser = null }) { Text("Отмена") }
            },
        )
    }

    if (showAddFriendsDialog) {
        AddFriendsDialog(
            profileState = state,
            directoryState = directoryState,
            profileViewModel = viewModel,
            directoryViewModel = directoryViewModel,
            onOpenUserProfile = onOpenUserProfile,
            onDismiss = {
                showAddFriendsDialog = false
                directoryViewModel.refreshRelationships()
            },
        )
    }
}

@Composable
private fun FriendUserCard(
    user: PublicUserProfile,
    showUnfollow: Boolean,
    onUnfollow: () -> Unit,
    onOpenProfile: () -> Unit,
    showFollowBack: Boolean,
    onFollowBack: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenProfile)) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    user.displayName?.takeIf(String::isNotBlank) ?: "Пользователь DailyBrain",
                    style = MaterialTheme.typography.titleMedium,
                )
                if (user.username.isNotBlank()) {
                    Text("@" + user.username, color = MaterialTheme.colorScheme.primary)
                }
            }
            if (showUnfollow) {
                IconButton(onClick = onUnfollow) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Отписаться",
                        tint = Color(0xFFC62828),
                    )
                }
            }
            if (showFollowBack) {
                IconButton(onClick = onFollowBack) {
                    Icon(
                        imageVector = Icons.Filled.PersonAdd,
                        contentDescription = "Подписаться в ответ",
                        tint = Color(0xFF2E7D32),
                    )
                }
            }
        }
    }
}

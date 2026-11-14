package com.jaroapps.dailybrain.feature.rockpaperscissors.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.jaroapps.dailybrain.R
import com.jaroapps.dailybrain.feature.profile.domain.model.ProfileCloudSessionStatus
import com.jaroapps.dailybrain.feature.profile.presentation.ProfileViewModel
import com.jaroapps.dailybrain.feature.profile.data.auth.GoogleSignInClient
import com.jaroapps.dailybrain.feature.rockpaperscissors.domain.model.CreateGameRoomRequest
import com.jaroapps.dailybrain.feature.rockpaperscissors.domain.model.GameRoom
import com.jaroapps.dailybrain.feature.rockpaperscissors.domain.model.GameRoomInvitation
import com.jaroapps.dailybrain.feature.rockpaperscissors.domain.model.GameRoomVisibility
import com.jaroapps.dailybrain.feature.rockpaperscissors.domain.model.RockPaperScissorsRules
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun GameRoomsScreen(
    onBack: () -> Unit,
    onOpenRoom: (String) -> Unit,
    viewModel: GameRoomsViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val profileViewModel: ProfileViewModel = koinViewModel()
    val profileState by profileViewModel.uiState.collectAsStateWithLifecycle()
    val googleSignInClient: GoogleSignInClient = koinInject()
    val coroutineScope = rememberCoroutineScope()
    var isGoogleSignInInProgress by remember { mutableStateOf(false) }
    var googleSignInError by remember { mutableStateOf<String?>(null) }
    fun startGoogleConnection() {
        googleSignInError = null
        if (state.cloudSessionStatus == ProfileCloudSessionStatus.REAUTH_REQUIRED) {
            profileViewModel.reauthenticateGoogleAccount()
        } else {
            if (isGoogleSignInInProgress) return
            coroutineScope.launch {
                isGoogleSignInInProgress = true
                try {
                    val result = googleSignInClient.signIn()
                    val expectedSubject = state.profile?.googleSubject
                    if (expectedSubject != null && result.account.subject != expectedSubject) {
                        googleSignInError = "Выберите Google-аккаунт, уже привязанный к этому профилю."
                    } else {
                        profileViewModel.linkGoogleAccount(result)
                    }
                } catch (error: Exception) {
                    googleSignInError = error.message ?: "Не удалось подключить Google-аккаунт."
                } finally {
                    isGoogleSignInInProgress = false
                }
            }
        }
    }
    val isAuthWorking = isGoogleSignInInProgress ||
        profileState.isGoogleLinking || profileState.isGoogleReauthenticating
    val authErrorMessage = googleSignInError ?: profileState.googleReauthenticationError
    var selectedTab by remember { mutableIntStateOf(0) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showJoinDialog by remember { mutableStateOf(false) }
    var roomToInvite by remember { mutableStateOf<GameRoom?>(null) }
    var showAuthDialog by remember { mutableStateOf(false) }
    var authPromptDismissed by remember { mutableStateOf(false) }

    LaunchedEffect(state.cloudSessionStatus, state.profile) {
        if (state.profile != null && state.cloudSessionStatus != ProfileCloudSessionStatus.READY &&
            !authPromptDismissed
        ) showAuthDialog = true
    }
    LaunchedEffect(state.openRoomId) {
        state.openRoomId?.let {
            viewModel.consumeOpenRoom()
            onOpenRoom(it)
        }
    }
    LaunchedEffect(showCreateDialog, showJoinDialog, roomToInvite) {
        if (roomToInvite != null) viewModel.loadFriends()
    }
    LaunchedEffect(state.actionError, state.actionMessage, state.authError) {
        if (state.actionError != null || state.actionMessage != null || state.authError != null) {
            // The view model keeps messages until the next action; the screen renders them inline.
        }
    }

    BackHandler { onBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Камень, ножницы, бумага") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Назад") }
                },
                actions = {
                    TextButton(onClick = viewModel::refresh) { Text("Обновить") }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (state.cloudSessionStatus == ProfileCloudSessionStatus.READY) {
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Доступные комнаты") })
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Ваши комнаты") })
                }

                if (state.invitations.isNotEmpty() && selectedTab == 0) {
                    Text("Приглашения для вас", fontWeight = FontWeight.Bold)
                    state.invitations.forEach { invitation ->
                        InvitationCard(
                            invitation = invitation,
                            enabled = !state.isWorking,
                            onAccept = { viewModel.acceptInvitation(invitation.invitationId) },
                            onDecline = { viewModel.declineInvitation(invitation.invitationId) },
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { showCreateDialog = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorResource(R.color.color_button_green),
                            contentColor = colorResource(R.color.color_on_accent),
                        ),
                    ) { Text("Создать комнату") }
                    OutlinedButton(
                        onClick = { showJoinDialog = true },
                        modifier = Modifier.weight(1f),
                    ) { Text("Войти по коду") }
                }

                if (state.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                } else if (state.roomsError != null) {
                    Text(state.roomsError.orEmpty(), color = MaterialTheme.colorScheme.error)
                    OutlinedButton(onClick = viewModel::refresh) { Text("Повторить") }
                } else {
                    val rooms = if (selectedTab == 0) state.openRooms else state.myRooms
                    if (rooms.isEmpty()) {
                        Text(
                            if (selectedTab == 0) "Сейчас нет доступных открытых комнат. Создайте свою!"
                            else "Вы пока не состоите ни в одной комнате.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            items(rooms, key = { it.roomId }) { room ->
                                GameRoomCard(
                                    room = room,
                                    isMyRoom = selectedTab == 1,
                                    isJoined = state.profile?.firebaseUid?.let { it in room.memberUids } == true,
                                    enabled = !state.isWorking,
                                    onOpen = { onOpenRoom(room.roomId) },
                                    onJoin = { viewModel.joinOpenRoom(room.roomId) },
                                    onInvite = { roomToInvite = room },
                                )
                            }
                        }
                    }
                }
            } else {
                Text(
                    if (state.cloudSessionStatus == ProfileCloudSessionStatus.REAUTH_REQUIRED) {
                        "Для этого профиля требуется восстановить подключение к ранее привязанному Google-аккаунту."
                    } else {
                        "Многопользовательская игра требует подключения Google-аккаунта. Остальные функции DailyBrain останутся доступны без авторизации."
                    },
                    style = MaterialTheme.typography.bodyLarge,
                )
                Button(
                    onClick = ::startGoogleConnection,
                    enabled = !isAuthWorking,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorResource(R.color.color_button_green),
                        contentColor = colorResource(R.color.color_on_accent),
                    ),
                ) {
                    if (isAuthWorking) CircularProgressIndicator()
                    else Text(
                        if (state.cloudSessionStatus == ProfileCloudSessionStatus.REAUTH_REQUIRED) {
                            "Восстановить подключение"
                        } else "Подключить Google"
                    )
                }
                authErrorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Позже") }
            }

            state.actionError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            state.actionMessage?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        }
    }

    if (showAuthDialog) {
        AlertDialog(
            onDismissRequest = {
                showAuthDialog = false
                authPromptDismissed = true
            },
            title = { Text("Подключите Google-аккаунт") },
            text = {
                Text(
                    "Для игры с другими пользователями нужны Google и Firebase. Ваш локальный профиль, тренировки и статистика сохранятся.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showAuthDialog = false
                    startGoogleConnection()
                }) { Text("Подключить") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAuthDialog = false
                    authPromptDismissed = true
                }) { Text("Позже") }
            },
        )
    }

    if (showCreateDialog) {
        CreateRoomDialog(
            isWorking = state.isWorking,
            onDismiss = { showCreateDialog = false },
            onCreate = {
                showCreateDialog = false
                viewModel.createRoom(it)
            },
        )
    }
    if (showJoinDialog) {
        JoinRoomDialog(
            isWorking = state.isWorking,
            onDismiss = { showJoinDialog = false },
            onJoin = {
                showJoinDialog = false
                viewModel.joinRoomByCode(it)
            },
        )
    }
    roomToInvite?.let { room ->
        InviteUserDialog(
            room = room,
            state = state,
            onLoadFriends = viewModel::loadFriends,
            onSearch = viewModel::searchUserToInvite,
            onInvite = { user ->
                viewModel.inviteUser(room.roomId, user)
                roomToInvite = null
            },
            onDismiss = {
                roomToInvite = null
                viewModel.clearMessages()
            },
        )
    }
}

@Composable
private fun GameRoomCard(
    room: GameRoom,
    isMyRoom: Boolean,
    isJoined: Boolean,
    enabled: Boolean,
    onOpen: () -> Unit,
    onJoin: () -> Unit,
    onInvite: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(room.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                (if (room.isPublic) "Открытая" else "Приватная") +
                    " · " + room.memberCount + "/" + room.maxParticipants + " участников",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text("Игра до " + room.targetScore + " очков", color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (!room.inviteCode.isNullOrBlank() && isMyRoom) {
                Text("Код приглашения: " + room.inviteCode, fontWeight = FontWeight.SemiBold)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = if (isJoined) onOpen else onJoin,
                    enabled = enabled,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorResource(R.color.color_button_green),
                        contentColor = colorResource(R.color.color_on_accent),
                    ),
                ) { Text(if (isJoined) "Открыть" else "Вступить") }
                if (isMyRoom) {
                    OutlinedButton(onClick = onInvite, enabled = enabled) { Text("Пригласить") }
                }
            }
        }
    }
}

@Composable
private fun InvitationCard(
    invitation: GameRoomInvitation,
    enabled: Boolean,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(invitation.roomName, fontWeight = FontWeight.SemiBold)
            Text("Приглашает " + invitation.inviterName, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onAccept,
                    enabled = enabled,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorResource(R.color.color_button_green),
                        contentColor = colorResource(R.color.color_on_accent),
                    ),
                ) { Text("Вступить") }
                OutlinedButton(onClick = onDecline, enabled = enabled, modifier = Modifier.weight(1f)) {
                    Text("Отклонить")
                }
            }
        }
    }
}

@Composable
private fun CreateRoomDialog(
    isWorking: Boolean,
    onDismiss: () -> Unit,
    onCreate: (CreateGameRoomRequest) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var visibility by remember { mutableStateOf(GameRoomVisibility.PUBLIC) }
    var maxParticipants by remember { mutableIntStateOf(RockPaperScissorsRules.DEFAULT_MAX_PARTICIPANTS) }
    var targetScore by remember { mutableIntStateOf(RockPaperScissorsRules.DEFAULT_TARGET_SCORE) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новая игровая комната") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(RockPaperScissorsRules.MAX_ROOM_NAME_LENGTH) },
                    label = { Text("Название комнаты") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Доступ")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = visibility == GameRoomVisibility.PUBLIC, onClick = { visibility = GameRoomVisibility.PUBLIC })
                    Text("Открытая")
                    Spacer(Modifier.weight(1f))
                    RadioButton(selected = visibility == GameRoomVisibility.PRIVATE, onClick = { visibility = GameRoomVisibility.PRIVATE })
                    Text("Приватная")
                }
                Text("Максимум участников: " + maxParticipants)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(2, 4, 6, 8).forEach { count ->
                        OutlinedButton(
                            onClick = { maxParticipants = count },
                            colors = if (maxParticipants == count) ButtonDefaults.outlinedButtonColors(
                                contentColor = colorResource(R.color.color_button_green),
                            ) else ButtonDefaults.outlinedButtonColors(),
                        ) { Text(count.toString()) }
                    }
                }
                Text("Игра до " + targetScore + " очков")
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(10, 25, 50, 100).forEach { score ->
                        OutlinedButton(
                            onClick = { targetScore = score },
                            colors = if (targetScore == score) ButtonDefaults.outlinedButtonColors(
                                contentColor = colorResource(R.color.color_button_green),
                            ) else ButtonDefaults.outlinedButtonColors(),
                        ) { Text(score.toString()) }
                    }
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !isWorking,
                onClick = {
                    runCatching {
                        RockPaperScissorsRules.validateRoomRequest(
                            CreateGameRoomRequest(name, visibility, maxParticipants, targetScore),
                        )
                    }.onSuccess(onCreate).onFailure {
                        error = it.message ?: "Проверьте настройки комнаты."
                    }
                },
            ) { Text("Создать") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
    )
}

@Composable
private fun JoinRoomDialog(
    isWorking: Boolean,
    onDismiss: () -> Unit,
    onJoin: (String) -> Unit,
) {
    var code by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Войти по коду") },
        text = {
            OutlinedTextField(
                value = code,
                onValueChange = { code = it.uppercase().filter(Char::isLetterOrDigit).take(8) },
                label = { Text("Код приглашения") },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(enabled = !isWorking && code.length == 8, onClick = { onJoin(code) }) {
                Text("Вступить")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
    )
}

@Composable
private fun InviteUserDialog(
    room: GameRoom,
    state: GameRoomsUiState,
    onLoadFriends: () -> Unit,
    onSearch: (String) -> Unit,
    onInvite: (com.jaroapps.dailybrain.feature.profile.domain.model.PublicUserProfile) -> Unit,
    onDismiss: () -> Unit,
) {
    var username by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Пригласить в «" + room.name + "»") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Ваши подписки", fontWeight = FontWeight.SemiBold)
                when {
                    state.isLoadingFriends -> CircularProgressIndicator()
                    state.friends.isEmpty() -> Text("Список подписок пуст.")
                    else -> LazyColumn(
                        modifier = Modifier.fillMaxWidth().height(130.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        items(state.friends, key = { it.firebaseUid }) { friend ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(friend.username, fontWeight = FontWeight.SemiBold)
                                    friend.displayName?.takeIf(String::isNotBlank)?.let {
                                        Text(it, style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                                TextButton(onClick = { onInvite(friend) }, enabled = !state.isWorking) {
                                    Text("Пригласить")
                                }
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Уникальный ник") },
                    singleLine = true,
                )
                Button(
                    onClick = { onSearch(username) },
                    enabled = !state.isSearchingUser && username.trim().removePrefix("@").length in 3..20,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (state.isSearchingUser) "Ищем..." else "Найти")
                }
                state.inviteSearchResult?.let { user ->
                    Text(user.username, fontWeight = FontWeight.SemiBold)
                    TextButton(onClick = { onInvite(user) }, enabled = !state.isWorking) {
                        Text("Отправить приглашение")
                    }
                }
                state.actionError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Закрыть") } },
    )
}

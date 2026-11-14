package com.jaroapps.dailybrain.feature.rockpaperscissors.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.runtime.DisposableEffect
import com.jaroapps.dailybrain.R
import com.jaroapps.dailybrain.feature.profile.domain.model.PublicUserProfile
import com.jaroapps.dailybrain.feature.rockpaperscissors.domain.model.CreateGameRoomRequest
import com.jaroapps.dailybrain.feature.rockpaperscissors.domain.model.GameChatMessage
import com.jaroapps.dailybrain.feature.rockpaperscissors.domain.model.GameMatchHistoryItem
import com.jaroapps.dailybrain.feature.rockpaperscissors.domain.model.GameParticipant
import com.jaroapps.dailybrain.feature.rockpaperscissors.domain.model.GamePairState
import com.jaroapps.dailybrain.feature.rockpaperscissors.domain.model.GameRoom
import com.jaroapps.dailybrain.feature.rockpaperscissors.domain.model.GameRoomVisibility
import com.jaroapps.dailybrain.feature.rockpaperscissors.domain.model.RockPaperScissorsMove
import com.jaroapps.dailybrain.feature.rockpaperscissors.domain.model.RockPaperScissorsOutcome
import org.koin.androidx.compose.koinViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val GameRoomTimeFormatter = DateTimeFormatter.ofPattern("dd.MM · HH:mm", Locale.getDefault())

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun GameRoomScreen(
    onBack: () -> Unit,
    onOpenUserProfile: (String) -> Unit,
    viewModel: GameRoomViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showRules by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }
    var showLeaveConfirmation by remember { mutableStateOf(false) }
    var showInviteDialog by remember { mutableStateOf(false) }
    var selectedParticipant by remember { mutableStateOf<GameParticipant?>(null) }
    val snapshot = state.snapshot
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> viewModel.onScreenResumed()
                Lifecycle.Event.ON_PAUSE -> viewModel.onScreenPaused()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        viewModel.onScreenResumed()
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.onScreenPaused()
        }
    }

    BackHandler(enabled = state.isChatOpen) { viewModel.closeChat() }
    LaunchedEffect(state.exitRequested) { if (state.exitRequested) onBack() }
    LaunchedEffect(showInviteDialog) {
        if (showInviteDialog) viewModel.loadFriends()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(snapshot?.room?.name ?: "Игровая комната")
                        snapshot?.let {
                            Text(
                                "Раунд " + (it.room.currentRoundId.toIntOrNull() ?: 1),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.Close, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = { showRules = true }) {
                        Icon(Icons.Filled.Info, contentDescription = "Правила игры")
                    }
                    if (snapshot?.room?.ownerUid == state.firebaseUid) {
                        IconButton(onClick = { showSettings = true }) {
                            Icon(Icons.Filled.Settings, contentDescription = "Настройки комнаты")
                        }
                    }
                    IconButton(onClick = {
                        viewModel.loadHistory()
                        showHistory = true
                    }) { Icon(Icons.Filled.History, contentDescription = "История ходов") }
                    IconButton(onClick = viewModel::openChat) {
                        Box {
                            Icon(Icons.Filled.Forum, contentDescription = "Чат комнаты")
                            if (snapshot?.hasUnreadMessages == true) {
                                Box(
                                    Modifier.align(Alignment.TopEnd).size(9.dp)
                                        .clip(CircleShape).background(MaterialTheme.colorScheme.error),
                                )
                            }
                        }
                    }
                    IconButton(onClick = { showLeaveConfirmation = true }) {
                        Icon(Icons.Filled.Logout, contentDescription = "Покинуть комнату")
                    }
                },
            )
        },
    ) { padding ->
        when {
            state.isLoading -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            snapshot == null -> Column(
                Modifier.fillMaxSize().padding(padding).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(state.errorMessage ?: "Не удалось открыть комнату.")
                OutlinedButton(onClick = viewModel::refresh) { Text("Повторить") }
            }

            else -> {
                val currentUid = state.firebaseUid.orEmpty()
                val opponents = snapshot.participants.filterNot { it.firebaseUid == currentUid }
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item {
                        RoomScoreboard(
                            participants = snapshot.participants,
                            targetScore = snapshot.room.targetScore,
                            currentUid = currentUid,
                            onSelectParticipant = { selectedParticipant = it },
                        )
                    }
                    item {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("Ваши матчи", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            IconButton(onClick = viewModel::refresh) {
                                Icon(Icons.Filled.Refresh, contentDescription = "Синхронизировать")
                            }
                        }
                    }
                    if (opponents.isEmpty()) {
                        item {
                            Card(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Ожидаем других участников", fontWeight = FontWeight.SemiBold)
                                    Text(
                                        "Пригласите друзей или поделитесь кодом комнаты, чтобы начать игру.",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    OutlinedButton(onClick = { showInviteDialog = true }) { Text("Пригласить пользователя") }
                                }
                            }
                        }
                    } else {
                        items(opponents, key = { it.firebaseUid }) { opponent ->
                            PairGameCard(
                                opponent = opponent,
                                pair = snapshot.pairs.firstOrNull { it.opponentUid == opponent.firebaseUid },
                                currentUid = currentUid,
                                isPendingOffline = opponent.firebaseUid in state.pendingOpponentUids,
                                enabled = !state.isWorking,
                                onMoveSelected = { viewModel.submitMove(opponent.firebaseUid, it) },
                                onOpenProfile = { onOpenUserProfile(opponent.firebaseUid) },
                            )
                        }
                    }
                    item {
                        OutlinedButton(
                            onClick = { showInviteDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Пригласить участника") }
                        state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                        Spacer(Modifier.height(12.dp))
                    }
                }
            }
        }
    }

    if (showRules) {
        AlertDialog(
            onDismissRequest = { showRules = false },
            title = { Text("Правила игры") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Вы играете отдельный матч против каждого участника комнаты.")
                    Text("Камень побеждает ножницы, ножницы побеждают бумагу, бумага побеждает камень.")
                    Text("Победа приносит 3 очка, ничья — 2 очка, поражение — 1 очко.")
                    Text("Выбор соперника скрыт, пока оба игрока не сделают ход.")
                    Text("Раунд завершается, когда участник достигает целевого количества очков. После этого начинается новый раунд.")
                }
            },
            confirmButton = { TextButton(onClick = { showRules = false }) { Text("Понятно") } },
        )
    }

    if (showSettings && snapshot != null) {
        EditGameRoomDialog(
            room = snapshot.room,
            isWorking = state.isWorking,
            onDismiss = { showSettings = false },
            onSave = {
                showSettings = false
                viewModel.updateRoom(it)
            },
        )
    }

    if (showLeaveConfirmation) {
        AlertDialog(
            onDismissRequest = { showLeaveConfirmation = false },
            title = { Text("Покинуть комнату?") },
            text = { Text("Вы больше не будете участвовать в текущем раунде. История завершённых матчей комнаты сохранится.") },
            confirmButton = {
                TextButton(
                    enabled = !state.isWorking,
                    onClick = {
                        showLeaveConfirmation = false
                        viewModel.leaveRoom()
                    },
                ) { Text("Покинуть", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showLeaveConfirmation = false }) { Text("Отмена") } },
        )
    }

    if (showHistory) {
        AlertDialog(
            onDismissRequest = { showHistory = false },
            title = { Text("История текущего раунда") },
            text = {
                when {
                    state.isHistoryLoading -> Box(
                        Modifier.fillMaxWidth().height(180.dp),
                        contentAlignment = Alignment.Center,
                    ) { CircularProgressIndicator() }
                    state.history.isEmpty() -> Text("В этом раунде ещё нет завершённых матчей.")
                    else -> LazyColumn(
                        Modifier.fillMaxWidth().height(360.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(state.history, key = { it.matchId }) { match -> MatchHistoryCard(match) }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showHistory = false }) { Text("Закрыть") } },
        )
    }

    if (state.isChatOpen) {
        GameChatDialog(
            messages = state.chatMessages,
            currentUid = state.firebaseUid,
            error = state.chatError,
            onSend = viewModel::sendChatMessage,
            onDismiss = viewModel::closeChat,
        )
    }

    if (showInviteDialog) {
        InviteRoomUserDialog(
            state = state,
            onLoadFriends = viewModel::loadFriends,
            onSearch = viewModel::searchUserToInvite,
            onInvite = {
                viewModel.inviteUser(it)
                showInviteDialog = false
            },
            onDismiss = {
                showInviteDialog = false
                viewModel.clearError()
            },
        )
    }

    selectedParticipant?.let { participant ->
        ParticipantStatsDialog(
            participant = participant,
            onOpenProfile = {
                selectedParticipant = null
                onOpenUserProfile(participant.firebaseUid)
            },
            onDismiss = { selectedParticipant = null },
        )
    }

    snapshot?.roundAnnouncement?.let { announcement ->
        AlertDialog(
            onDismissRequest = { viewModel.acknowledgeRoundAnnouncement(announcement.roundId) },
            title = { Text("Раунд завершён!") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (announcement.winnerUid == null) Text("В этом раунде получилась ничья.")
                    else Text("Победитель: " + (announcement.winnerName ?: "участник"))
                    Text("Результат: " + announcement.winningScore + " очков.")
                    Text("Начался новый раунд. Счёт участников обнулён.")
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.acknowledgeRoundAnnouncement(announcement.roundId) }) {
                    Text("Продолжить")
                }
            },
        )
    }
}

@Composable
private fun RoomScoreboard(
    participants: List<GameParticipant>,
    targetScore: Int,
    currentUid: String,
    onSelectParticipant: (GameParticipant) -> Unit,
) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Текущие результаты", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            participants.forEachIndexed { index, participant ->
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                        .clickable { onSelectParticipant(participant) }
                        .padding(vertical = 5.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text((index + 1).toString() + ".", modifier = Modifier.width(24.dp), fontWeight = FontWeight.Bold)
                    Column(Modifier.weight(1f)) {
                        Text(
                            participant.name + if (participant.firebaseUid == currentUid) " (вы)" else "",
                            fontWeight = if (participant.firebaseUid == currentUid) FontWeight.Bold else FontWeight.Normal,
                        )
                        Text(
                            participant.wins.toString() + " побед · " + participant.draws +
                                " ничьих · " + participant.losses + " поражений",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        participant.score.toString() + " / " + targetScore,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colorResource(R.color.color_button_green),
                    )
                }
            }
            Text("Нажмите на участника, чтобы посмотреть статистику.", style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun PairGameCard(
    opponent: GameParticipant,
    pair: GamePairState?,
    currentUid: String,
    isPendingOffline: Boolean,
    enabled: Boolean,
    onMoveSelected: (RockPaperScissorsMove) -> Unit,
    onOpenProfile: () -> Unit,
) {
    val waiting = pair?.status == "WAITING"
    val resolved = pair?.status == "RESOLVED"
    val ownSubmitted = waiting && currentUid in pair?.submittedUids.orEmpty()
    val opponentSubmitted = waiting && opponent.firebaseUid in pair?.submittedUids.orEmpty()
    val moveLocked = ownSubmitted || isPendingOffline

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(opponent.name, fontWeight = FontWeight.Bold, modifier = Modifier.clickable(onClick = onOpenProfile))
                Text("Счёт: " + opponent.score, color = colorResource(R.color.color_button_green))
            }
            when {
                isPendingOffline -> Text("Ваш ход сохранён на устройстве и ожидает отправки.", color = MaterialTheme.colorScheme.tertiary)
                ownSubmitted && opponentSubmitted -> Text("Оба участника сделали ход. Подводим итоги…")
                ownSubmitted -> Text("Ваш ход принят. Ожидаем ход соперника.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                opponentSubmitted -> Text("Соперник уже сделал ход. Его выбор пока скрыт.", color = MaterialTheme.colorScheme.tertiary)
                resolved && pair != null -> {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Ваш выбор", style = MaterialTheme.typography.labelSmall)
                            Text(moveEmoji(pair.ownMove), style = MaterialTheme.typography.headlineMedium)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Выбор соперника", style = MaterialTheme.typography.labelSmall)
                            Text(moveEmoji(pair.opponentMove), style = MaterialTheme.typography.headlineMedium)
                        }
                    }
                    Text(
                        when (pair.outcome) {
                            RockPaperScissorsOutcome.WIN -> "Вы выиграли матч: +" + (pair.ownPoints ?: 3) + " очка"
                            RockPaperScissorsOutcome.DRAW -> "Ничья: +" + (pair.ownPoints ?: 2) + " очка"
                            RockPaperScissorsOutcome.LOSS -> "Вы проиграли матч: +" + (pair.ownPoints ?: 1) + " очко"
                            null -> "Матч завершён."
                        },
                        fontWeight = FontWeight.SemiBold,
                        color = when (pair.outcome) {
                            RockPaperScissorsOutcome.WIN -> colorResource(R.color.color_button_green)
                            RockPaperScissorsOutcome.LOSS -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurface
                        },
                    )
                }
                else -> Text("Выберите ход против этого участника.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (!moveLocked) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MoveButton("✊ Камень", enabled, Modifier.weight(1f)) { onMoveSelected(RockPaperScissorsMove.ROCK) }
                    MoveButton("✌️ Ножницы", enabled, Modifier.weight(1f)) { onMoveSelected(RockPaperScissorsMove.SCISSORS) }
                    MoveButton("✋ Бумага", enabled, Modifier.weight(1f)) { onMoveSelected(RockPaperScissorsMove.PAPER) }
                }
            }
            pair?.takeIf { it.matchNumber > 0 }?.let {
                Text("Матч №" + it.matchNumber, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun MoveButton(label: String, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 3.dp, vertical = 8.dp),
    ) { Text(label, textAlign = TextAlign.Center, style = MaterialTheme.typography.labelMedium) }
}

@Composable
private fun MatchHistoryCard(match: GameMatchHistoryItem) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                runCatching {
                    GameRoomTimeFormatter.format(
                        Instant.ofEpochMilli(match.createdAtEpochMs).atZone(ZoneId.systemDefault()),
                    )
                }.getOrDefault(""),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(match.firstName + " " + moveEmoji(match.firstMove) + " — " + moveEmoji(match.secondMove) + " " + match.secondName)
            Text(match.firstPoints.toString() + " : " + match.secondPoints + " очков", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun GameChatDialog(
    messages: List<GameChatMessage>,
    currentUid: String?,
    error: String?,
    onSend: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var draft by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Чат комнаты") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LazyColumn(
                    Modifier.fillMaxWidth().height(300.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(messages, key = { it.messageId }) { message ->
                        val own = message.senderUid == currentUid
                        Column(
                            Modifier.fillMaxWidth(),
                            horizontalAlignment = if (own) Alignment.End else Alignment.Start,
                        ) {
                            Text(message.senderName, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                            Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(12.dp)) {
                                Text(message.text, Modifier.padding(9.dp))
                            }
                        }
                    }
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = draft,
                        onValueChange = { draft = it.take(500) },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Сообщение") },
                        maxLines = 3,
                    )
                    TextButton(
                        enabled = draft.isNotBlank(),
                        onClick = {
                            onSend(draft)
                            draft = ""
                        },
                    ) { Text("Отправить") }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Закрыть") } },
    )
}

@Composable
private fun ParticipantStatsDialog(
    participant: GameParticipant,
    onOpenProfile: () -> Unit,
    onDismiss: () -> Unit,
) {
    val total = participant.rockCount + participant.paperCount + participant.scissorsCount
    fun percent(count: Int): Int = if (total == 0) 0 else count * 100 / total
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Статистика: " + participant.name) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Результаты текущего раунда")
                Text("Победы: " + participant.wins)
                Text("Ничьи: " + participant.draws)
                Text("Поражения: " + participant.losses)
                Text("Набрано очков: " + participant.score)
                Text("Распределение ходов")
                Text("✊ Камень: " + percent(participant.rockCount) + "%")
                Text("✌️ Ножницы: " + percent(participant.scissorsCount) + "%")
                Text("✋ Бумага: " + percent(participant.paperCount) + "%")
            }
        },
        confirmButton = { TextButton(onClick = onOpenProfile) { Text("Открыть профиль") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Закрыть") } },
    )
}

@Composable
private fun EditGameRoomDialog(
    room: GameRoom,
    isWorking: Boolean,
    onDismiss: () -> Unit,
    onSave: (CreateGameRoomRequest) -> Unit,
) {
    var name by remember(room.roomId) { mutableStateOf(room.name) }
    var visibility by remember(room.roomId) { mutableStateOf(room.visibility) }
    var maxParticipants by remember(room.roomId) { mutableStateOf(room.maxParticipants) }
    var targetScore by remember(room.roomId) { mutableStateOf(room.targetScore) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Настройки комнаты") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it.take(40) }, label = { Text("Название") }, singleLine = true)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = visibility == GameRoomVisibility.PUBLIC, onClick = { visibility = GameRoomVisibility.PUBLIC })
                    Text("Открытая")
                    Spacer(Modifier.width(8.dp))
                    RadioButton(selected = visibility == GameRoomVisibility.PRIVATE, onClick = { visibility = GameRoomVisibility.PRIVATE })
                    Text("Приватная")
                }
                Text("Максимум участников: " + maxParticipants)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(2, 4, 6, 8).forEach { value ->
                        OutlinedButton(onClick = { maxParticipants = value }, enabled = value >= room.memberCount) { Text(value.toString()) }
                    }
                }
                Text("Игра до " + targetScore + " очков")
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(10, 25, 50, 100).forEach { value ->
                        OutlinedButton(onClick = { targetScore = value }) { Text(value.toString()) }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !isWorking && name.isNotBlank() && maxParticipants >= room.memberCount,
                onClick = { onSave(CreateGameRoomRequest(name, visibility, maxParticipants, targetScore)) },
            ) { Text("Сохранить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
    )
}

@Composable
private fun InviteRoomUserDialog(
    state: GameRoomUiState,
    onLoadFriends: () -> Unit,
    onSearch: (String) -> Unit,
    onInvite: (PublicUserProfile) -> Unit,
    onDismiss: () -> Unit,
) {
    var username by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Пригласить участника") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Ваши подписки", fontWeight = FontWeight.SemiBold)
                when {
                    state.isLoadingFriends -> CircularProgressIndicator()
                    state.friends.isEmpty() -> Text("Список подписок пуст.")
                    else -> LazyColumn(
                        Modifier.fillMaxWidth().height(130.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        items(state.friends, key = { it.firebaseUid }) { friend ->
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
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
                OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("Уникальный ник") }, singleLine = true)
                Button(
                    onClick = { onSearch(username) },
                    enabled = !state.isSearchingUser && username.trim().removePrefix("@").length in 3..20,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(if (state.isSearchingUser) "Ищем..." else "Найти") }
                state.inviteSearchResult?.let { user ->
                    Text(user.username, fontWeight = FontWeight.SemiBold)
                    TextButton(onClick = { onInvite(user) }, enabled = !state.isWorking) { Text("Отправить приглашение") }
                }
                state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Закрыть") } },
    )
}

private fun moveEmoji(move: RockPaperScissorsMove?): String = when (move) {
    RockPaperScissorsMove.ROCK -> "✊"
    RockPaperScissorsMove.SCISSORS -> "✌️"
    RockPaperScissorsMove.PAPER -> "✋"
    null -> "—"
}

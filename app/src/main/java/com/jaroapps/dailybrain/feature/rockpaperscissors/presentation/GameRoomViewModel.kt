package com.jaroapps.dailybrain.feature.rockpaperscissors.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jaroapps.dailybrain.core.database.ActiveUserProvider
import com.jaroapps.dailybrain.core.database.AppDatabase
import com.jaroapps.dailybrain.feature.profile.domain.model.PublicUserProfile
import com.jaroapps.dailybrain.feature.profile.domain.repository.FirebaseAuthenticationRepository
import com.jaroapps.dailybrain.feature.profile.domain.repository.UserDirectoryRepository
import com.jaroapps.dailybrain.feature.profile.domain.usecase.ObserveUserProfileUseCase
import com.jaroapps.dailybrain.feature.rockpaperscissors.domain.model.CreateGameRoomRequest
import com.jaroapps.dailybrain.feature.rockpaperscissors.domain.model.GameChatMessage
import com.jaroapps.dailybrain.feature.rockpaperscissors.domain.model.GameMatchHistoryItem
import com.jaroapps.dailybrain.feature.rockpaperscissors.domain.model.GameRoomSnapshot
import com.jaroapps.dailybrain.feature.rockpaperscissors.domain.model.RockPaperScissorsMove
import com.jaroapps.dailybrain.feature.rockpaperscissors.domain.repository.RockPaperScissorsGameplayRepository
import com.jaroapps.dailybrain.feature.rockpaperscissors.domain.usecase.ManageGameRoomsUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GameRoomUiState(
    val roomId: String = "",
    val firebaseUid: String? = null,
    val snapshot: GameRoomSnapshot? = null,
    val pendingOpponentUids: Set<String> = emptySet(),
    val isLoading: Boolean = true,
    val isWorking: Boolean = false,
    val errorMessage: String? = null,
    val history: List<GameMatchHistoryItem> = emptyList(),
    val isHistoryLoading: Boolean = false,
    val chatMessages: List<GameChatMessage> = emptyList(),
    val isChatOpen: Boolean = false,
    val chatError: String? = null,
    val exitRequested: Boolean = false,
    val inviteSearchResult: PublicUserProfile? = null,
    val isSearchingUser: Boolean = false,
    val friends: List<PublicUserProfile> = emptyList(),
    val isLoadingFriends: Boolean = false,
)

class GameRoomViewModel(
    savedStateHandle: SavedStateHandle,
    observeUserProfile: ObserveUserProfileUseCase,
    private val firebaseAuthenticationRepository: FirebaseAuthenticationRepository,
    private val gameplayRepository: RockPaperScissorsGameplayRepository,
    private val manageGameRooms: ManageGameRoomsUseCase,
    private val userDirectoryRepository: UserDirectoryRepository,
    private val activeUserProvider: ActiveUserProvider,
    private val database: AppDatabase,
) : ViewModel() {
    private val roomId: String = checkNotNull(savedStateHandle["roomId"])
    private val _uiState = MutableStateFlow(GameRoomUiState(roomId = roomId))
    val uiState = _uiState.asStateFlow()

    private var currentName = "Пользователь"
    private var roomJob: Job? = null
    private var pendingJob: Job? = null
    private var chatJob: Job? = null
    private var currentUid: String? = null
    private var isScreenActive = false
    private var lastMarkedChatReadAtEpochMs = 0L

    init {
        viewModelScope.launch {
            observeUserProfile().collect { profile ->
                currentName = profile.username?.takeIf(String::isNotBlank)?.let { "@" + it }
                    ?: profile.displayName?.takeIf(String::isNotBlank)
                    ?: profile.generatedAlias
            }
        }
        viewModelScope.launch {
            kotlinx.coroutines.flow.combine(
                observeUserProfile(),
                firebaseAuthenticationRepository.observeCurrentUserId(),
            ) { profile, firebaseUid ->
                profile.firebaseUid?.takeIf { it == firebaseUid && it.isNotBlank() }
            }.collect { uid ->
                if (uid == currentUid) return@collect
                currentUid = uid
                stopObservers()
                lastMarkedChatReadAtEpochMs = 0L
                if (uid == null) {
                    _uiState.update {
                        it.copy(
                            firebaseUid = null,
                            snapshot = null,
                            isLoading = false,
                            pendingOpponentUids = emptySet(),
                            chatMessages = emptyList(),
                            errorMessage = "Восстановите подключение Google-аккаунта для доступа к комнате.",
                        )
                    }
                    return@collect
                }
                _uiState.update {
                    it.copy(firebaseUid = uid, isLoading = true, errorMessage = null, pendingOpponentUids = emptySet())
                }
                if (isScreenActive) startObservers(uid)
            }
        }
    }

    fun onScreenResumed() {
        if (isScreenActive) return
        isScreenActive = true
        currentUid?.let(::startObservers)
    }

    fun onScreenPaused() {
        if (!isScreenActive) return
        isScreenActive = false
        stopObservers()
    }

    private fun startObservers(uid: String) {
        if (roomJob?.isActive == true) return
        roomJob = viewModelScope.launch {
            try {
                gameplayRepository.observeRoom(uid, roomId).collect { snapshot ->
                    _uiState.update { it.copy(snapshot = snapshot, isLoading = false, errorMessage = null) }
                    if (_uiState.value.isChatOpen) markChatRead(snapshot.latestMessageAtEpochMs)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = error.message ?: "Не удалось загрузить комнату.")
                }
            }
        }
        pendingJob = viewModelScope.launch {
            activeUserProvider.observeUserId()
                .flatMapLatest { localUserId ->
                    database.pendingGameMoveDao().observePendingForRoom(localUserId, roomId)
                }
                .collect { pending ->
                    _uiState.update { it.copy(pendingOpponentUids = pending.map { move -> move.opponentUid }.toSet()) }
                }
        }
        viewModelScope.launch {
            runCatching { gameplayRepository.syncPendingMoves(uid, roomId) }
                .onFailure { error ->
                    if (error !is CancellationException) {
                        _uiState.update { it.copy(errorMessage = error.message ?: "Не удалось синхронизировать ходы.") }
                    }
                }
        }
        if (_uiState.value.isChatOpen) startChatListener(uid)
    }

    private fun stopObservers() {
        roomJob?.cancel()
        roomJob = null
        pendingJob?.cancel()
        pendingJob = null
        chatJob?.cancel()
        chatJob = null
    }

    fun submitMove(opponentUid: String, move: RockPaperScissorsMove) {
        val uid = currentUid ?: return
        if (!isScreenActive) return
        val roundId = _uiState.value.snapshot?.room?.currentRoundId ?: return
        val pair = _uiState.value.snapshot?.pairs?.firstOrNull { it.opponentUid == opponentUid }
        val alreadySubmitted = pair?.status == "WAITING" && uid in pair.submittedUids
        if (opponentUid in _uiState.value.pendingOpponentUids || alreadySubmitted) return
        viewModelScope.launch {
            _uiState.update { it.copy(errorMessage = null) }
            try {
                gameplayRepository.submitMove(uid, roomId, roundId, opponentUid, move)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _uiState.update { it.copy(errorMessage = error.message ?: "Не удалось отправить ход.") }
            }
        }
    }

    fun refresh() {
        val uid = currentUid ?: return
        if (!isScreenActive) return
        viewModelScope.launch {
            runCatching { gameplayRepository.syncPendingMoves(uid, roomId) }
                .onFailure { error ->
                    if (error !is CancellationException) {
                        _uiState.update { it.copy(errorMessage = error.message ?: "Не удалось синхронизировать ходы.") }
                    }
                }
        }
    }

    fun loadHistory() {
        val uid = currentUid ?: return
        if (!isScreenActive) return
        viewModelScope.launch {
            _uiState.update { it.copy(isHistoryLoading = true, errorMessage = null) }
            try {
                val history = gameplayRepository.getRoundHistory(uid, roomId).sortedBy { it.createdAtEpochMs }
                _uiState.update { it.copy(history = history, isHistoryLoading = false) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _uiState.update { it.copy(isHistoryLoading = false, errorMessage = error.message ?: "Не удалось загрузить историю.") }
            }
        }
    }

    fun openChat() {
        val uid = currentUid ?: return
        _uiState.update { it.copy(isChatOpen = true, chatError = null) }
        if (!isScreenActive) return
        markChatRead(_uiState.value.snapshot?.latestMessageAtEpochMs ?: 0L)
        startChatListener(uid)
    }

    private fun startChatListener(uid: String) {
        if (!isScreenActive || chatJob?.isActive == true) return
        chatJob = viewModelScope.launch {
            try {
                gameplayRepository.observeChat(uid, roomId).collect { messages ->
                    _uiState.update { it.copy(chatMessages = messages, chatError = null) }
                    markChatRead(messages.lastOrNull()?.createdAtEpochMs ?: 0L)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _uiState.update { it.copy(chatError = error.message ?: "Не удалось загрузить чат.") }
            }
        }
    }

    private fun markChatRead(timestamp: Long) {
        val uid = currentUid ?: return
        if (timestamp <= 0L || timestamp <= lastMarkedChatReadAtEpochMs || !isScreenActive) return
        lastMarkedChatReadAtEpochMs = timestamp
        viewModelScope.launch {
            runCatching { gameplayRepository.markChatRead(uid, roomId, timestamp) }
                .onFailure { error ->
                    if (error !is CancellationException) {
                        lastMarkedChatReadAtEpochMs = minOf(lastMarkedChatReadAtEpochMs, timestamp - 1L)
                    }
                }
        }
    }

    fun closeChat() {
        chatJob?.cancel()
        chatJob = null
        _uiState.update { it.copy(isChatOpen = false) }
    }

    fun sendChatMessage(text: String) {
        val uid = currentUid ?: return
        val content = text.trim()
        if (content.isEmpty()) return
        viewModelScope.launch {
            try {
                gameplayRepository.sendChatMessage(uid, roomId, content)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _uiState.update { it.copy(chatError = error.message ?: "Не удалось отправить сообщение.") }
            }
        }
    }

    fun acknowledgeRoundAnnouncement(roundId: String) {
        val uid = currentUid ?: return
        viewModelScope.launch {
            runCatching { gameplayRepository.acknowledgeRoundAnnouncement(uid, roomId, roundId) }
                .onFailure { error ->
                    _uiState.update { it.copy(errorMessage = error.message ?: "Не удалось закрыть объявление.") }
                }
        }
    }

    fun leaveRoom() {
        val uid = currentUid ?: return
        performAction {
            manageGameRooms.leaveRoom(uid, roomId)
            _uiState.update { it.copy(exitRequested = true) }
        }
    }

    fun updateRoom(request: CreateGameRoomRequest) {
        val uid = currentUid ?: return
        performAction { manageGameRooms.updateRoom(uid, roomId, request) }
    }


    fun loadFriends() {
        val uid = currentUid ?: return
        if (_uiState.value.isLoadingFriends || _uiState.value.friends.isNotEmpty()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingFriends = true, errorMessage = null) }
            try {
                val friends = userDirectoryRepository.getFollowing(uid)
                if (currentUid == uid) _uiState.update { it.copy(friends = friends, isLoadingFriends = false) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _uiState.update { it.copy(isLoadingFriends = false, errorMessage = error.message ?: "Не удалось загрузить список подписок.") }
            }
        }
    }

    fun searchUserToInvite(username: String) {
        val uid = currentUid ?: return
        val normalized = username.trim().removePrefix("@").lowercase()
        if (normalized.length !in 3..20) {
            _uiState.update { it.copy(errorMessage = "Введите ник длиной от 3 до 20 символов.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSearchingUser = true, inviteSearchResult = null, errorMessage = null) }
            try {
                val user = userDirectoryRepository.findUserByUsername(uid, normalized)
                _uiState.update {
                    it.copy(isSearchingUser = false, inviteSearchResult = user, errorMessage = if (user == null) "Пользователь не найден." else null)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _uiState.update { it.copy(isSearchingUser = false, errorMessage = error.message ?: "Не удалось найти пользователя.") }
            }
        }
    }

    fun inviteUser(user: PublicUserProfile) {
        val uid = currentUid ?: return
        performAction {
            manageGameRooms.inviteUser(uid, currentName, roomId, user)
            _uiState.update { it.copy(inviteSearchResult = null) }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null, chatError = null) }
    }

    private fun performAction(action: suspend () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isWorking = true, errorMessage = null) }
            try {
                action()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _uiState.update { it.copy(errorMessage = error.message ?: "Не удалось выполнить действие.") }
            } finally {
                _uiState.update { it.copy(isWorking = false) }
            }
        }
    }

    override fun onCleared() {
        stopObservers()
        super.onCleared()
    }
}

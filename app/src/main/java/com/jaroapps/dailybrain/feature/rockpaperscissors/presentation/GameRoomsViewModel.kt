package com.jaroapps.dailybrain.feature.rockpaperscissors.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jaroapps.dailybrain.feature.profile.data.auth.GoogleSignInProvider
import com.jaroapps.dailybrain.feature.profile.domain.model.LinkGoogleAccountResult
import com.jaroapps.dailybrain.feature.profile.domain.model.ProfileCloudSessionStatus
import com.jaroapps.dailybrain.feature.profile.domain.model.PublicUserProfile
import com.jaroapps.dailybrain.feature.profile.domain.model.UserProfile
import com.jaroapps.dailybrain.feature.profile.domain.repository.FirebaseAuthenticationRepository
import com.jaroapps.dailybrain.feature.profile.domain.usecase.LinkGoogleAccountUseCase
import com.jaroapps.dailybrain.feature.profile.domain.usecase.ObserveUserProfileUseCase
import com.jaroapps.dailybrain.feature.profile.domain.usecase.ReauthenticateGoogleAccountUseCase
import com.jaroapps.dailybrain.feature.profile.domain.repository.UserDirectoryRepository
import com.jaroapps.dailybrain.feature.rockpaperscissors.domain.model.CreateGameRoomRequest
import com.jaroapps.dailybrain.feature.rockpaperscissors.domain.model.GameRoom
import com.jaroapps.dailybrain.feature.rockpaperscissors.domain.model.GameRoomInvitation
import com.jaroapps.dailybrain.feature.rockpaperscissors.domain.model.RockPaperScissorsRules
import com.jaroapps.dailybrain.feature.rockpaperscissors.domain.usecase.ManageGameRoomsUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GameRoomsUiState(
    val profile: UserProfile? = null,
    val cloudSessionStatus: ProfileCloudSessionStatus = ProfileCloudSessionStatus.NOT_LINKED,
    val isLoading: Boolean = false,
    val isAuthenticating: Boolean = false,
    val authError: String? = null,
    val roomsError: String? = null,
    val openRooms: List<GameRoom> = emptyList(),
    val myRooms: List<GameRoom> = emptyList(),
    val invitations: List<GameRoomInvitation> = emptyList(),
    val isWorking: Boolean = false,
    val actionError: String? = null,
    val actionMessage: String? = null,
    val openRoomId: String? = null,
    val inviteSearchResult: PublicUserProfile? = null,
    val isSearchingUser: Boolean = false,
)

class GameRoomsViewModel(
    observeUserProfile: ObserveUserProfileUseCase,
    private val firebaseAuthenticationRepository: FirebaseAuthenticationRepository,
    private val googleSignInProvider: GoogleSignInProvider,
    private val linkGoogleAccount: LinkGoogleAccountUseCase,
    private val reauthenticateGoogleAccount: ReauthenticateGoogleAccountUseCase,
    private val userDirectoryRepository: UserDirectoryRepository,
    private val manageGameRooms: ManageGameRoomsUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(GameRoomsUiState())
    val uiState = _uiState.asStateFlow()
    private var lastLoadedUid: String? = null

    init {
        viewModelScope.launch {
            combine(
                observeUserProfile(),
                firebaseAuthenticationRepository.observeCurrentUserId(),
            ) { profile, firebaseUid -> profile to firebaseUid }
                .collect { (profile, firebaseUid) ->
                    val status = when {
                        profile.firebaseUid.isNullOrBlank() -> ProfileCloudSessionStatus.NOT_LINKED
                        profile.firebaseUid == firebaseUid -> ProfileCloudSessionStatus.READY
                        else -> ProfileCloudSessionStatus.REAUTH_REQUIRED
                    }
                    _uiState.update { it.copy(profile = profile, cloudSessionStatus = status) }
                    if (status == ProfileCloudSessionStatus.READY && lastLoadedUid != firebaseUid) {
                        lastLoadedUid = firebaseUid
                        refresh()
                    } else if (status != ProfileCloudSessionStatus.READY) {
                        lastLoadedUid = null
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                openRooms = emptyList(),
                                myRooms = emptyList(),
                                invitations = emptyList(),
                            )
                        }
                    }
                }
        }
    }

    fun authenticate() {
        val state = _uiState.value
        val profile = state.profile ?: return
        if (state.isAuthenticating) return
        viewModelScope.launch {
            _uiState.update { it.copy(isAuthenticating = true, authError = null) }
            try {
                if (!profile.firebaseUid.isNullOrBlank() && !profile.googleSubject.isNullOrBlank()) {
                    reauthenticateGoogleAccount(profile.localAccountId)
                } else {
                    val signIn = googleSignInProvider.signIn()
                    when (linkGoogleAccount(profile.localAccountId, signIn.account, signIn.idToken)) {
                        LinkGoogleAccountResult.LINKED -> Unit
                        LinkGoogleAccountResult.ALREADY_LINKED_TO_ANOTHER_PROFILE ->
                            throw IllegalStateException("Этот Google-аккаунт уже привязан к другому профилю.")
                        LinkGoogleAccountResult.FIREBASE_NOT_CONFIGURED ->
                            throw IllegalStateException("Firebase не настроен. Проверьте app/google-services.json.")
                        is LinkGoogleAccountResult.FIREBASE_AUTH_FAILED ->
                            throw IllegalStateException("Не удалось выполнить вход в Firebase.")
                        LinkGoogleAccountResult.SAVE_FAILED ->
                            throw IllegalStateException("Не удалось сохранить подключение профиля.")
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _uiState.update { it.copy(authError = error.message ?: "Не удалось подключить Google-аккаунт.") }
            } finally {
                _uiState.update { it.copy(isAuthenticating = false) }
            }
        }
    }

    fun refresh() {
        val uid = readyUid() ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, roomsError = null) }
            try {
                val result = coroutineScope {
                    val open = async { manageGameRooms.openRooms(uid) }
                    val mine = async { manageGameRooms.myRooms(uid) }
                    val invites = async { manageGameRooms.invitations(uid) }
                    Triple(open.await(), mine.await(), invites.await())
                }
                if (isCurrentSession(uid)) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            openRooms = result.first,
                            myRooms = result.second,
                            invitations = result.third,
                            roomsError = null,
                        )
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        roomsError = error.message ?: "Не удалось загрузить игровые комнаты.",
                    )
                }
            }
        }
    }

    fun createRoom(request: CreateGameRoomRequest) {
        val uid = readyUid() ?: return
        val ownerName = currentUserName()
        performRoomAction {
            val room = manageGameRooms.createRoom(uid, ownerName, request)
            _uiState.update { it.copy(openRoomId = room.roomId) }
            refresh()
        }
    }

    fun joinOpenRoom(roomId: String) {
        val uid = readyUid() ?: return
        performRoomAction {
            val room = manageGameRooms.joinOpenRoom(uid, currentUserName(), roomId)
            _uiState.update { it.copy(openRoomId = room.roomId) }
            refresh()
        }
    }

    fun joinRoomByCode(code: String) {
        val uid = readyUid() ?: return
        performRoomAction {
            val room = manageGameRooms.joinRoomByCode(uid, currentUserName(), code)
            _uiState.update { it.copy(openRoomId = room.roomId) }
            refresh()
        }
    }

    fun acceptInvitation(invitationId: String) {
        val uid = readyUid() ?: return
        performRoomAction {
            val room = manageGameRooms.acceptInvitation(uid, invitationId)
            _uiState.update { it.copy(openRoomId = room.roomId) }
            refresh()
        }
    }

    fun declineInvitation(invitationId: String) {
        val uid = readyUid() ?: return
        performRoomAction {
            manageGameRooms.declineInvitation(uid, invitationId)
            _uiState.update { it.copy(actionMessage = "Приглашение отклонено.") }
            refresh()
        }
    }

    fun searchUserToInvite(username: String) {
        val uid = readyUid() ?: return
        val normalized = username.trim().removePrefix("@").lowercase()
        if (normalized.length !in 3..20) {
            _uiState.update { it.copy(actionError = "Введите ник длиной от 3 до 20 символов.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSearchingUser = true, inviteSearchResult = null, actionError = null) }
            try {
                val user = userDirectoryRepository.findUserByUsername(uid, normalized)
                _uiState.update {
                    it.copy(
                        isSearchingUser = false,
                        inviteSearchResult = user,
                        actionError = if (user == null) "Пользователь не найден." else null,
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(isSearchingUser = false, actionError = error.message ?: "Не удалось найти пользователя.")
                }
            }
        }
    }

    fun inviteUser(roomId: String, user: PublicUserProfile) {
        val uid = readyUid() ?: return
        performRoomAction {
            manageGameRooms.inviteUser(uid, currentUserName(), roomId, user)
            _uiState.update { it.copy(actionMessage = "Приглашение отправлено.", inviteSearchResult = null) }
        }
    }

    fun leaveRoom(roomId: String) {
        val uid = readyUid() ?: return
        performRoomAction {
            manageGameRooms.leaveRoom(uid, roomId)
            _uiState.update { it.copy(actionMessage = "Вы покинули комнату.") }
            refresh()
        }
    }

    fun updateRoom(roomId: String, request: CreateGameRoomRequest) {
        val uid = readyUid() ?: return
        performRoomAction {
            manageGameRooms.updateRoom(uid, roomId, request)
            _uiState.update { it.copy(actionMessage = "Настройки комнаты сохранены.") }
            refresh()
        }
    }

    fun consumeOpenRoom() {
        _uiState.update { it.copy(openRoomId = null) }
    }

    fun clearMessages() {
        _uiState.update { it.copy(actionError = null, actionMessage = null, authError = null) }
    }

    private fun performRoomAction(action: suspend () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isWorking = true, actionError = null, actionMessage = null) }
            try {
                action()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(actionError = error.message ?: "Не удалось выполнить действие.")
                }
            } finally {
                _uiState.update { it.copy(isWorking = false) }
            }
        }
    }

    private fun readyUid(): String? {
        val state = _uiState.value
        return state.profile?.firebaseUid?.takeIf {
            state.cloudSessionStatus == ProfileCloudSessionStatus.READY &&
                firebaseAuthenticationRepository.currentUserId() == it
        }
    }

    private fun isCurrentSession(uid: String): Boolean =
        firebaseAuthenticationRepository.currentUserId() == uid &&
            _uiState.value.profile?.firebaseUid == uid

    private fun currentUserName(): String {
        val profile = _uiState.value.profile
        return profile?.username?.takeIf(String::isNotBlank)?.let { "@$it" }
            ?: profile?.effectiveDisplayName
            ?: "Пользователь"
    }
}

package com.jaroapps.dailybrain.feature.rockpaperscissors.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jaroapps.dailybrain.feature.profile.domain.repository.FirebaseAuthenticationRepository
import com.jaroapps.dailybrain.feature.profile.domain.usecase.ObserveUserProfileUseCase
import com.jaroapps.dailybrain.feature.rockpaperscissors.domain.model.GameRoomInvitation
import com.jaroapps.dailybrain.feature.rockpaperscissors.domain.usecase.ManageGameRoomsUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GameInvitationPromptUiState(
    val invitation: GameRoomInvitation? = null,
    val isAccepting: Boolean = false,
    val errorMessage: String? = null,
    val openRoomId: String? = null,
    val isSessionReady: Boolean = false,
)

class GameInvitationPromptViewModel(
    context: Context,
    observeUserProfile: ObserveUserProfileUseCase,
    private val firebaseAuthenticationRepository: FirebaseAuthenticationRepository,
    private val manageGameRooms: ManageGameRoomsUseCase,
) : ViewModel() {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )
    private val _uiState = MutableStateFlow(GameInvitationPromptUiState())
    val uiState = _uiState.asStateFlow()
    private var activeFirebaseUid: String? = null
    private var checkedForUid: String? = null
    private var isChecking = false

    init {
        viewModelScope.launch {
            combine(
                observeUserProfile(),
                firebaseAuthenticationRepository.observeCurrentUserId(),
            ) { profile, firebaseUid ->
                profile.firebaseUid?.takeIf { it.isNotBlank() && it == firebaseUid }
            }.collect { uid ->
                activeFirebaseUid = uid
                if (uid != checkedForUid) {
                    checkedForUid = null
                    _uiState.update {
                        it.copy(
                            invitation = null,
                            errorMessage = null,
                            isSessionReady = uid != null,
                        )
                    }
                }
            }
        }
    }

    fun checkForPrompt() {
        val uid = activeFirebaseUid ?: return
        if (checkedForUid == uid || isChecking || _uiState.value.invitation != null) return
        if (firebaseAuthenticationRepository.currentUserId() != uid) return
        isChecking = true
        viewModelScope.launch {
            try {
                val pending = manageGameRooms.invitations(uid)
                val unseen = pending.firstOrNull { invitation ->
                    !preferences.getBoolean(preferenceKey(uid, invitation.invitationId), false)
                }
                checkedForUid = uid
                if (unseen != null) {
                    // Mark before showing so a rotation or recomposition cannot show it twice.
                    preferences.edit()
                        .putBoolean(preferenceKey(uid, unseen.invitationId), true)
                        .apply()
                    _uiState.update { it.copy(invitation = unseen, errorMessage = null) }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // A failed network request must not consume the one-time prompt opportunity.
                checkedForUid = null
            } finally {
                isChecking = false
            }
        }
    }

    fun dismiss() {
        _uiState.update { it.copy(invitation = null, errorMessage = null) }
    }

    fun acceptInvitation() {
        val uid = activeFirebaseUid ?: return
        val invitation = _uiState.value.invitation ?: return
        if (_uiState.value.isAccepting) return
        if (firebaseAuthenticationRepository.currentUserId() != uid) {
            _uiState.update {
                it.copy(
                    isAccepting = false,
                    errorMessage = "Восстановите подключение Google-аккаунта.",
                )
            }
            return
        }
        _uiState.update { it.copy(isAccepting = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                val room = manageGameRooms.acceptInvitation(uid, invitation.invitationId)
                _uiState.update { it.copy(invitation = null, openRoomId = room.roomId) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        isAccepting = false,
                        errorMessage = error.message ?: "Не удалось принять приглашение.",
                    )
                }
            } finally {
                _uiState.update { it.copy(isAccepting = false) }
            }
        }
    }

    fun consumeOpenRoom() {
        _uiState.update { it.copy(openRoomId = null) }
    }

    private fun preferenceKey(uid: String, invitationId: String): String =
        "shown:" + uid + ":" + invitationId

    private companion object {
        const val PREFERENCES_NAME = "game_invitation_prompts"
    }
}

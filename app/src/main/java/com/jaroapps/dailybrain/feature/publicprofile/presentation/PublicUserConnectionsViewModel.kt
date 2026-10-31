package com.jaroapps.dailybrain.feature.publicprofile.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jaroapps.dailybrain.feature.profile.domain.model.PublicUserProfile
import com.jaroapps.dailybrain.feature.profile.domain.repository.UserDirectoryRepository
import com.jaroapps.dailybrain.feature.profile.domain.usecase.ObserveUserProfileUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.update

data class PublicUserConnectionsUiState(
    val users: List<PublicUserProfile> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

class PublicUserConnectionsViewModel(
    private val observeUserProfile: ObserveUserProfileUseCase,
    private val userDirectoryRepository: UserDirectoryRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(PublicUserConnectionsUiState())
    val uiState: StateFlow<PublicUserConnectionsUiState> = _uiState

    fun load(profileFirebaseUid: String, connectionType: String) {
        viewModelScope.launch {
            _uiState.value = PublicUserConnectionsUiState(isLoading = true)
            try {
                val viewerUid = observeUserProfile().first().firebaseUid
                    ?: throw IllegalStateException("Для просмотра списков подключите Google-аккаунт.")
                val users = when (connectionType) {
                    FOLLOWING -> userDirectoryRepository.getPublicFollowing(viewerUid, profileFirebaseUid)
                    FOLLOWERS -> userDirectoryRepository.getPublicFollowers(viewerUid, profileFirebaseUid)
                    else -> throw IllegalArgumentException("Неизвестный тип списка пользователей.")
                }
                _uiState.value = PublicUserConnectionsUiState(users = users)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _uiState.update { it.copy(isLoading = false, error = error.message ?: "Не удалось загрузить список.") }
            }
        }
    }

    companion object {
        const val FOLLOWING = "following"
        const val FOLLOWERS = "followers"
    }
}

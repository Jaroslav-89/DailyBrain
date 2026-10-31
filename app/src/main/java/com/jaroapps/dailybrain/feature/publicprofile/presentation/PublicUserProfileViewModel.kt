package com.jaroapps.dailybrain.feature.publicprofile.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jaroapps.dailybrain.feature.profile.domain.model.PublicUserProfile
import com.jaroapps.dailybrain.feature.profile.domain.repository.UserDirectoryRepository
import com.jaroapps.dailybrain.feature.profile.domain.usecase.ObserveUserProfileUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PublicUserProfileUiState(
    val viewerFirebaseUid: String? = null,
    val user: PublicUserProfile? = null,
    val followingUsers: List<PublicUserProfile> = emptyList(),
    val followerUsers: List<PublicUserProfile> = emptyList(),
    val isSelf: Boolean = false,
    val isFollowing: Boolean = false,
    val isLoading: Boolean = false,
    val isUpdatingFollow: Boolean = false,
    val error: String? = null,
)

class PublicUserProfileViewModel(
    private val observeUserProfile: ObserveUserProfileUseCase,
    private val userDirectoryRepository: UserDirectoryRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(PublicUserProfileUiState())
    val uiState: StateFlow<PublicUserProfileUiState> = _uiState
    private var requestedProfileUid: String? = null
    private var loadJob: kotlinx.coroutines.Job? = null

    fun load(profileFirebaseUid: String) {
        if (requestedProfileUid == profileFirebaseUid && (_uiState.value.isLoading || _uiState.value.user != null)) return
        requestedProfileUid = profileFirebaseUid
        loadJob?.cancel()
        loadJob = viewModelScope.launch { loadProfile(profileFirebaseUid) }
    }

    private suspend fun loadProfile(profileFirebaseUid: String) {
        _uiState.value = PublicUserProfileUiState(isLoading = true)
        try {
            val activeProfile = observeUserProfile().first()
            val viewerUid = activeProfile.firebaseUid
                ?: throw IllegalStateException("Для просмотра профилей подключите Google-аккаунт.")
            val user = userDirectoryRepository.getPublicUserProfile(viewerUid, profileFirebaseUid)
                ?: throw IllegalStateException("Профиль пользователя не найден.")
            val self = viewerUid == profileFirebaseUid
            if (user.isRestricted) {
                _uiState.value = PublicUserProfileUiState(
                    viewerFirebaseUid = viewerUid,
                    user = user,
                    isSelf = false,
                    isFollowing = false,
                    isLoading = false,
                )
                return
            }
            val result = coroutineScope {
                val following = async { userDirectoryRepository.getPublicFollowing(viewerUid, profileFirebaseUid) }
                val followers = async { userDirectoryRepository.getPublicFollowers(viewerUid, profileFirebaseUid) }
                val isFollowing = async {
                    if (self) false else userDirectoryRepository.isFollowing(viewerUid, profileFirebaseUid)
                }
                Triple(following.await(), followers.await(), isFollowing.await())
            }
            _uiState.value = PublicUserProfileUiState(
                viewerFirebaseUid = viewerUid,
                user = user,
                followingUsers = result.first,
                followerUsers = result.second,
                isSelf = self,
                isFollowing = result.third,
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            _uiState.update { it.copy(isLoading = false, error = error.message ?: "Не удалось загрузить профиль.") }
        }
    }

    fun toggleFollow() {
        val state = _uiState.value
        val viewerUid = state.viewerFirebaseUid ?: return
        val user = state.user ?: return
        if (state.isSelf || state.isUpdatingFollow) return
        viewModelScope.launch {
            _uiState.update { it.copy(isUpdatingFollow = true, error = null) }
            try {
                if (state.isFollowing) {
                    userDirectoryRepository.unfollowUser(viewerUid, user.firebaseUid)
                } else {
                    userDirectoryRepository.followUser(viewerUid, user.firebaseUid)
                }
                loadProfile(user.firebaseUid)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        isUpdatingFollow = false,
                        error = error.message ?: "Не удалось изменить подписку.",
                    )
                }
            }
        }
    }
}

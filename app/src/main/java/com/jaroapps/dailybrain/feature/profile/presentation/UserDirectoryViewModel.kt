package com.jaroapps.dailybrain.feature.profile.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jaroapps.dailybrain.feature.profile.domain.model.InviteCodePolicy
import com.jaroapps.dailybrain.feature.profile.domain.model.PublicUserProfile
import com.jaroapps.dailybrain.feature.profile.domain.model.UserProfile
import com.jaroapps.dailybrain.feature.profile.domain.model.UsernamePolicy
import com.jaroapps.dailybrain.feature.profile.domain.repository.FirebaseAuthenticationRepository
import com.jaroapps.dailybrain.feature.profile.domain.repository.FirebaseSessionMismatchException
import com.jaroapps.dailybrain.feature.profile.domain.repository.UserDirectoryNotConfiguredException
import com.jaroapps.dailybrain.feature.profile.domain.repository.UserDirectoryProfileMissingException
import com.jaroapps.dailybrain.feature.profile.domain.repository.UserDirectoryRepository
import com.jaroapps.dailybrain.feature.profile.domain.repository.UserProfileRepository
import com.jaroapps.dailybrain.feature.profile.domain.repository.UsernameTakenException
import com.jaroapps.dailybrain.feature.profile.domain.usecase.ObserveUserProfileUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class UserDirectoryUiState(
    val profile: UserProfile? = null,
    val isSavingUsername: Boolean = false,
    val usernameMessage: String? = null,
    val usernameError: Boolean = false,
    val isUnlinkingGoogle: Boolean = false,
    val unlinkError: String? = null,
    val isSearching: Boolean = false,
    val searchHasRun: Boolean = false,
    val searchResult: PublicUserProfile? = null,
    val searchError: String? = null,
    val isFollowingSearchResult: Boolean = false,
    val isFollowingAction: Boolean = false,
    val followError: String? = null,
    val followingUsers: List<PublicUserProfile> = emptyList(),
    val followerUsers: List<PublicUserProfile> = emptyList(),
    val isLoadingRelationships: Boolean = false,
    val relationshipError: String? = null,
    val inviteCode: String? = null,
    val isLoadingInviteCode: Boolean = false,
    val inviteCodeError: String? = null,
)

private data class UserDirectoryActionState(
    val isSavingUsername: Boolean = false,
    val usernameMessage: String? = null,
    val usernameError: Boolean = false,
    val isUnlinkingGoogle: Boolean = false,
    val unlinkError: String? = null,
    val isSearching: Boolean = false,
    val searchHasRun: Boolean = false,
    val searchResult: PublicUserProfile? = null,
    val searchError: String? = null,
    val isFollowingSearchResult: Boolean = false,
    val isFollowingAction: Boolean = false,
    val followError: String? = null,
    val followingUsers: List<PublicUserProfile> = emptyList(),
    val followerUsers: List<PublicUserProfile> = emptyList(),
    val isLoadingRelationships: Boolean = false,
    val relationshipError: String? = null,
    val inviteCode: String? = null,
    val isLoadingInviteCode: Boolean = false,
    val inviteCodeError: String? = null,
)

class UserDirectoryViewModel(
    observeUserProfile: ObserveUserProfileUseCase,
    private val userDirectoryRepository: UserDirectoryRepository,
    private val userProfileRepository: UserProfileRepository,
    private val firebaseAuthenticationRepository: FirebaseAuthenticationRepository,
) : ViewModel() {
    private val profile: StateFlow<UserProfile?> = observeUserProfile()
        .map<UserProfile, UserProfile?> { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    private val actions = MutableStateFlow(UserDirectoryActionState())
    private var dataOwnerUid: String? = null

    val uiState: StateFlow<UserDirectoryUiState> = combine(profile, actions) { activeProfile, action ->
        UserDirectoryUiState(
            profile = activeProfile,
            isSavingUsername = action.isSavingUsername,
            usernameMessage = action.usernameMessage,
            usernameError = action.usernameError,
            isUnlinkingGoogle = action.isUnlinkingGoogle,
            unlinkError = action.unlinkError,
            isSearching = action.isSearching,
            searchHasRun = action.searchHasRun,
            searchResult = action.searchResult,
            searchError = action.searchError,
            isFollowingSearchResult = action.isFollowingSearchResult,
            isFollowingAction = action.isFollowingAction,
            followError = action.followError,
            followingUsers = action.followingUsers,
            followerUsers = action.followerUsers,
            isLoadingRelationships = action.isLoadingRelationships,
            relationshipError = action.relationshipError,
            inviteCode = action.inviteCode,
            isLoadingInviteCode = action.isLoadingInviteCode,
            inviteCodeError = action.inviteCodeError,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserDirectoryUiState())

    fun resetUsernameError() {
        actions.value = actions.value.copy(usernameMessage = null, usernameError = false)
    }

    fun saveUsername(input: String, onResult: (Boolean) -> Unit) {
        val activeProfile = profile.value
        val uid = activeProfile?.firebaseUid
        if (activeProfile == null || uid == null) {
            actions.value = actions.value.copy(usernameMessage = "Сначала подключите Google-аккаунт.", usernameError = true)
            onResult(false)
            return
        }
        val normalized = UsernamePolicy.normalize(input)
        if (normalized.isNotEmpty() && !UsernamePolicy.isValid(normalized)) {
            actions.value = actions.value.copy(
                usernameMessage = "Используйте 3–20 латинских букв, цифр или символов подчёркивания.",
                usernameError = true,
            )
            onResult(false)
            return
        }
        if (normalized == activeProfile.username.orEmpty()) {
            resetUsernameError()
            onResult(true)
            return
        }

        actions.value = actions.value.copy(isSavingUsername = true, usernameMessage = null, usernameError = false)
        viewModelScope.launch {
            try {
                if (normalized.isNotEmpty() && !userDirectoryRepository.isUsernameAvailable(uid, normalized)) {
                    actions.value = actions.value.copy(
                        isSavingUsername = false,
                        usernameMessage = "Этот ник уже занят.",
                        usernameError = true,
                    )
                    onResult(false)
                    return@launch
                }
                userDirectoryRepository.updateUsername(uid, normalized)
                userProfileRepository.updateUsername(
                    localAccountId = activeProfile.localAccountId,
                    username = normalized.takeIf(String::isNotBlank),
                )
                actions.value = actions.value.copy(
                    isSavingUsername = false, usernameMessage = null, usernameError = false,
                )
                onResult(true)
            } catch (cancelled: CancellationException) {
                actions.value = actions.value.copy(isSavingUsername = false)
                throw cancelled
            } catch (error: Exception) {
                actions.value = actions.value.copy(
                    isSavingUsername = false, usernameMessage = errorMessage(error), usernameError = true,
                )
                onResult(false)
            }
        }
    }

    fun unlinkGoogleAccount(onUnlinked: () -> Unit) {
        val activeProfile = profile.value ?: return
        val uid = activeProfile.firebaseUid ?: return
        actions.value = actions.value.copy(isUnlinkingGoogle = true, unlinkError = null)
        viewModelScope.launch {
            var cloudProfileDeleted = false
            try {
                userDirectoryRepository.unlinkProfile(uid)
                cloudProfileDeleted = true
                userProfileRepository.unlinkGoogleAccount(activeProfile.localAccountId)
                firebaseAuthenticationRepository.signOut()
                actions.value = UserDirectoryActionState()
                onUnlinked()
            } catch (cancelled: CancellationException) {
                if (cloudProfileDeleted) firebaseAuthenticationRepository.signOut()
                actions.value = actions.value.copy(isUnlinkingGoogle = false)
                throw cancelled
            } catch (error: Exception) {
                if (cloudProfileDeleted) firebaseAuthenticationRepository.signOut()
                actions.value = actions.value.copy(
                    isUnlinkingGoogle = false,
                    unlinkError = if (cloudProfileDeleted) {
                        "Облачный профиль удалён, но локальную привязку не удалось очистить. Перезапустите приложение."
                    } else errorMessage(error),
                )
            }
        }
    }

    fun resetSearch() {
        actions.value = actions.value.copy(
            isSearching = false,
            searchHasRun = false,
            searchResult = null,
            searchError = null,
            isFollowingSearchResult = false,
            isFollowingAction = false,
            followError = null,
        )
    }

    fun searchUserByUsername(input: String) {
        val normalized = UsernamePolicy.normalize(input)
        if (!UsernamePolicy.isValid(normalized)) {
            actions.value = actions.value.copy(
                searchHasRun = false, searchResult = null,
                searchError = "Введите ник длиной 3–20 символов: латинские буквы, цифры или _.",
            )
            return
        }
        searchUser { uid -> userDirectoryRepository.findUserByUsername(uid, normalized) }
    }

    fun searchUserByInviteCode(input: String) {
        val normalized = InviteCodePolicy.normalize(input)
        if (!InviteCodePolicy.isValid(normalized)) {
            actions.value = actions.value.copy(
                searchHasRun = false, searchResult = null,
                searchError = "Введите корректный код приглашения из 8 символов.",
            )
            return
        }
        searchUser { uid -> userDirectoryRepository.findUserByInviteCode(uid, normalized) }
    }

    private fun searchUser(lookup: suspend (String) -> PublicUserProfile?) {
        val uid = profile.value?.firebaseUid
        if (uid == null) {
            actions.value = actions.value.copy(
                isSearching = false, searchHasRun = false, searchResult = null,
                searchError = "Для поиска пользователей подключите Google-аккаунт.",
            )
            return
        }
        actions.value = actions.value.copy(
            isSearching = true, searchHasRun = false, searchResult = null, searchError = null,
            isFollowingSearchResult = false, followError = null,
        )
        viewModelScope.launch {
            try {
                val result = lookup(uid)
                val following = when {
                    result == null -> false
                    result.firebaseUid == uid -> true
                    else -> userDirectoryRepository.isFollowing(uid, result.firebaseUid)
                }
                actions.value = actions.value.copy(
                    isSearching = false, searchHasRun = true, searchResult = result,
                    searchError = null, isFollowingSearchResult = following,
                )
            } catch (cancelled: CancellationException) {
                actions.value = actions.value.copy(isSearching = false)
                throw cancelled
            } catch (error: Exception) {
                actions.value = actions.value.copy(
                    isSearching = false, searchHasRun = false, searchResult = null,
                    searchError = errorMessage(error),
                )
            }
        }
    }

    fun followSearchResult() {
        val uid = profile.value?.firebaseUid
        val target = actions.value.searchResult
        if (uid == null || target == null || actions.value.isFollowingAction ||
            actions.value.isFollowingSearchResult
        ) return
        if (target.firebaseUid == uid) {
            actions.value = actions.value.copy(
                isFollowingSearchResult = true, followError = "Нельзя подписаться на собственный профиль.",
            )
            return
        }
        actions.value = actions.value.copy(isFollowingAction = true, followError = null)
        viewModelScope.launch {
            try {
                userDirectoryRepository.followUser(uid, target.firebaseUid)
                actions.value = actions.value.copy(
                    isFollowingAction = false, isFollowingSearchResult = true, followError = null,
                )
                refreshRelationshipsNow(uid)
            } catch (cancelled: CancellationException) {
                actions.value = actions.value.copy(isFollowingAction = false)
                throw cancelled
            } catch (error: Exception) {
                actions.value = actions.value.copy(isFollowingAction = false, followError = errorMessage(error))
            }
        }
    }

    fun unfollowUser(user: PublicUserProfile) {
        val uid = profile.value?.firebaseUid ?: return
        if (dataOwnerUid != uid) return
        actions.value = actions.value.copy(relationshipError = null)
        viewModelScope.launch {
            try {
                userDirectoryRepository.unfollowUser(uid, user.firebaseUid)
                if (dataOwnerUid == uid && profile.value?.firebaseUid == uid) {
                    actions.value = actions.value.copy(
                        followingUsers = actions.value.followingUsers.filterNot { it.firebaseUid == user.firebaseUid },
                        relationshipError = null,
                    )
                    refreshRelationshipsNow(uid)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (dataOwnerUid == uid && profile.value?.firebaseUid == uid) {
                    actions.value = actions.value.copy(relationshipError = errorMessage(error))
                }
            }
        }
    }

    fun followUser(user: PublicUserProfile) {
        val uid = profile.value?.firebaseUid ?: return
        if (dataOwnerUid != uid || user.firebaseUid == uid ||
            actions.value.followingUsers.any { it.firebaseUid == user.firebaseUid }
        ) return
        actions.value = actions.value.copy(relationshipError = null)
        viewModelScope.launch {
            try {
                userDirectoryRepository.followUser(uid, user.firebaseUid)
                if (dataOwnerUid == uid && profile.value?.firebaseUid == uid) {
                    refreshRelationshipsNow(uid)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (dataOwnerUid == uid && profile.value?.firebaseUid == uid) {
                    actions.value = actions.value.copy(relationshipError = errorMessage(error))
                }
            }
        }
    }

    fun blockUser(user: PublicUserProfile) {
        val uid = profile.value?.firebaseUid ?: return
        if (dataOwnerUid != uid || user.firebaseUid == uid) return
        actions.value = actions.value.copy(relationshipError = null)
        viewModelScope.launch {
            try {
                userDirectoryRepository.blockUser(uid, user.firebaseUid)
                if (dataOwnerUid == uid && profile.value?.firebaseUid == uid) {
                    refreshRelationshipsNow(uid)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (dataOwnerUid == uid && profile.value?.firebaseUid == uid) {
                    actions.value = actions.value.copy(relationshipError = errorMessage(error))
                }
            }
        }
    }

    fun refreshRelationships() {
        val uid = profile.value?.firebaseUid
        if (uid == null) {
            dataOwnerUid = null
            actions.value = actions.value.copy(
                followingUsers = emptyList(),
                followerUsers = emptyList(),
                isLoadingRelationships = false,
                relationshipError = null,
                inviteCode = null,
                isLoadingInviteCode = false,
                inviteCodeError = null,
                isSearching = false,
                searchHasRun = false,
                searchResult = null,
                searchError = null,
                isFollowingSearchResult = false,
                isFollowingAction = false,
                followError = null,
            )
            return
        }
        if (dataOwnerUid != uid) {
            dataOwnerUid = uid
            actions.value = actions.value.copy(
                followingUsers = emptyList(),
                followerUsers = emptyList(),
                relationshipError = null,
                inviteCode = null,
                isLoadingInviteCode = false,
                inviteCodeError = null,
                isSearching = false,
                searchHasRun = false,
                searchResult = null,
                searchError = null,
                isFollowingSearchResult = false,
                isFollowingAction = false,
                followError = null,
            )
        }
        actions.value = actions.value.copy(isLoadingRelationships = true, relationshipError = null)
        viewModelScope.launch {
            try {
                refreshRelationshipsNow(uid)
            } catch (cancelled: CancellationException) {
                if (dataOwnerUid == uid && profile.value?.firebaseUid == uid) {
                    actions.value = actions.value.copy(isLoadingRelationships = false)
                }
                throw cancelled
            } catch (error: Exception) {
                if (dataOwnerUid == uid && profile.value?.firebaseUid == uid) {
                    actions.value = actions.value.copy(
                        isLoadingRelationships = false,
                        relationshipError = errorMessage(error),
                    )
                }
            }
        }
    }

    private suspend fun refreshRelationshipsNow(uid: String) {
        val following = userDirectoryRepository.getFollowing(uid)
        val followers = userDirectoryRepository.getFollowers(uid)
        if (dataOwnerUid != uid || profile.value?.firebaseUid != uid) return
        actions.value = actions.value.copy(
            followingUsers = following,
            followerUsers = followers,
            isLoadingRelationships = false,
            relationshipError = null,
        )
    }

    fun loadInviteCode() {
        val activeProfile = profile.value
        val uid = activeProfile?.firebaseUid
        if (activeProfile == null || uid == null) {
            dataOwnerUid = null
            actions.value = actions.value.copy(
                inviteCode = null,
                isLoadingInviteCode = false,
                inviteCodeError = null,
            )
            return
        }

        val cachedCode = activeProfile.inviteCode?.takeIf(String::isNotBlank)
        if (dataOwnerUid != uid) {
            dataOwnerUid = uid
            actions.value = actions.value.copy(
                followingUsers = emptyList(),
                followerUsers = emptyList(),
                inviteCode = cachedCode,
                isLoadingInviteCode = false,
                inviteCodeError = null,
                isSearching = false,
                searchHasRun = false,
                searchResult = null,
                searchError = null,
                isFollowingSearchResult = false,
                isFollowingAction = false,
                followError = null,
            )
        }

        if (cachedCode != null) {
            actions.value = actions.value.copy(
                inviteCode = cachedCode,
                isLoadingInviteCode = false,
                inviteCodeError = null,
            )
            return
        }
        if (actions.value.inviteCode != null || actions.value.isLoadingInviteCode) return

        actions.value = actions.value.copy(isLoadingInviteCode = true, inviteCodeError = null)
        viewModelScope.launch {
            try {
                val code = userDirectoryRepository.getOrCreateInviteCode(uid)
                val currentProfile = profile.value
                if (
                    dataOwnerUid != uid ||
                    currentProfile?.firebaseUid != uid ||
                    currentProfile.localAccountId != activeProfile.localAccountId
                ) return@launch

                userProfileRepository.cacheInviteCode(
                    localAccountId = activeProfile.localAccountId,
                    firebaseUid = uid,
                    inviteCode = code,
                )
                if (
                    dataOwnerUid != uid ||
                    profile.value?.firebaseUid != uid ||
                    profile.value?.localAccountId != activeProfile.localAccountId
                ) return@launch

                actions.value = actions.value.copy(
                    inviteCode = code,
                    isLoadingInviteCode = false,
                    inviteCodeError = null,
                )
            } catch (cancelled: CancellationException) {
                if (dataOwnerUid == uid) actions.value = actions.value.copy(isLoadingInviteCode = false)
                throw cancelled
            } catch (error: Exception) {
                if (dataOwnerUid == uid && profile.value?.firebaseUid == uid) {
                    actions.value = actions.value.copy(
                        isLoadingInviteCode = false,
                        inviteCodeError = errorMessage(error),
                    )
                }
            }
        }
    }

    private fun errorMessage(error: Exception): String = when (error) {
        is UsernameTakenException -> "Этот ник уже занят."
        is FirebaseSessionMismatchException -> "Активный Google-аккаунт не совпадает с выбранным локальным профилем."
        is UserDirectoryNotConfiguredException -> "Firebase не настроен в приложении."
        is UserDirectoryProfileMissingException -> "Облачный профиль не найден. Переподключите Google-аккаунт."
        else -> "Не удалось связаться с сервером. Проверьте подключение и попробуйте ещё раз."
    }
}

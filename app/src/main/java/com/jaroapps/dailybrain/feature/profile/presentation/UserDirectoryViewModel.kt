package com.jaroapps.dailybrain.feature.profile.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jaroapps.dailybrain.feature.profile.domain.model.PublicUserProfile
import com.jaroapps.dailybrain.feature.profile.domain.model.UserProfile
import com.jaroapps.dailybrain.feature.profile.domain.model.UsernamePolicy
import com.jaroapps.dailybrain.feature.profile.domain.repository.FirebaseAuthenticationRepository
import com.jaroapps.dailybrain.feature.profile.domain.repository.FirebaseSessionMismatchException
import com.jaroapps.dailybrain.feature.profile.domain.repository.UserDirectoryNotConfiguredException
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
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserDirectoryUiState())

    fun resetUsernameError() {
        actions.value = actions.value.copy(usernameMessage = null, usernameError = false)
    }

    /**
     * Called by the main Save button. A changed non-empty username is checked first,
     * then claimed by a Firestore transaction that repeats the uniqueness check.
     */
    fun saveUsername(input: String, onResult: (Boolean) -> Unit) {
        val activeProfile = profile.value
        val firebaseUid = activeProfile?.firebaseUid
        if (activeProfile == null || firebaseUid == null) {
            actions.value = actions.value.copy(
                usernameMessage = "Сначала подключите Google-аккаунт.",
                usernameError = true,
            )
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

        actions.value = actions.value.copy(
            isSavingUsername = true,
            usernameMessage = null,
            usernameError = false,
        )
        viewModelScope.launch {
            try {
                if (normalized.isNotEmpty() &&
                    !userDirectoryRepository.isUsernameAvailable(firebaseUid, normalized)
                ) {
                    actions.value = actions.value.copy(
                        isSavingUsername = false,
                        usernameMessage = "Этот ник уже занят.",
                        usernameError = true,
                    )
                    onResult(false)
                    return@launch
                }

                // Transactional write repeats ownership/uniqueness checks to prevent races.
                userDirectoryRepository.updateUsername(firebaseUid, normalized)
                userProfileRepository.updateUsername(
                    localAccountId = activeProfile.localAccountId,
                    username = normalized.takeIf(String::isNotBlank),
                )
                actions.value = actions.value.copy(
                    isSavingUsername = false,
                    usernameMessage = null,
                    usernameError = false,
                )
                onResult(true)
            } catch (cancelled: CancellationException) {
                actions.value = actions.value.copy(isSavingUsername = false)
                throw cancelled
            } catch (error: Exception) {
                actions.value = actions.value.copy(
                    isSavingUsername = false,
                    usernameMessage = errorMessage(error),
                    usernameError = true,
                )
                onResult(false)
            }
        }
    }

    /** Deletes the cloud directory and releases the handle before clearing this local binding. */
    fun unlinkGoogleAccount(onUnlinked: () -> Unit) {
        val activeProfile = profile.value ?: return
        val firebaseUid = activeProfile.firebaseUid ?: return

        actions.value = actions.value.copy(isUnlinkingGoogle = true, unlinkError = null)
        viewModelScope.launch {
            var cloudProfileDeleted = false
            try {
                userDirectoryRepository.unlinkProfile(firebaseUid)
                cloudProfileDeleted = true
                userProfileRepository.unlinkGoogleAccount(activeProfile.localAccountId)
                firebaseAuthenticationRepository.signOut()
                actions.value = actions.value.copy(isUnlinkingGoogle = false, unlinkError = null)
                onUnlinked()
            } catch (cancelled: CancellationException) {
                if (cloudProfileDeleted) firebaseAuthenticationRepository.signOut()
                actions.value = actions.value.copy(isUnlinkingGoogle = false)
                throw cancelled
            } catch (error: Exception) {
                if (cloudProfileDeleted) {
                    firebaseAuthenticationRepository.signOut()
                }
                actions.value = actions.value.copy(
                    isUnlinkingGoogle = false,
                    unlinkError = if (cloudProfileDeleted) {
                        "Облачный профиль удалён, но локальную привязку не удалось очистить. Перезапустите приложение."
                    } else {
                        errorMessage(error)
                    },
                )
            }
        }
    }

    fun resetSearch() {
        actions.value = actions.value.copy(
            searchHasRun = false,
            searchResult = null,
            searchError = null,
        )
    }

    fun searchUserByUsername(input: String) {
        val firebaseUid = profile.value?.firebaseUid
        if (firebaseUid == null) {
            actions.value = actions.value.copy(
                searchHasRun = false,
                searchResult = null,
                searchError = "Для поиска пользователей подключите Google-аккаунт.",
            )
            return
        }
        val normalized = UsernamePolicy.normalize(input)
        if (!UsernamePolicy.isValid(normalized)) {
            actions.value = actions.value.copy(
                searchHasRun = false,
                searchResult = null,
                searchError = "Введите ник длиной 3–20 символов: латинские буквы, цифры или _.",
            )
            return
        }

        actions.value = actions.value.copy(
            isSearching = true,
            searchHasRun = false,
            searchResult = null,
            searchError = null,
        )
        viewModelScope.launch {
            try {
                val result = userDirectoryRepository.findUserByUsername(firebaseUid, normalized)
                actions.value = actions.value.copy(
                    isSearching = false,
                    searchHasRun = true,
                    searchResult = result,
                    searchError = null,
                )
            } catch (cancelled: CancellationException) {
                actions.value = actions.value.copy(isSearching = false)
                throw cancelled
            } catch (error: Exception) {
                actions.value = actions.value.copy(
                    isSearching = false,
                    searchHasRun = false,
                    searchResult = null,
                    searchError = errorMessage(error),
                )
            }
        }
    }

    private fun errorMessage(error: Exception): String = when (error) {
        is UsernameTakenException -> "Этот ник уже занят."
        is FirebaseSessionMismatchException ->
            "Активный Google-аккаунт не совпадает с выбранным локальным профилем."
        is UserDirectoryNotConfiguredException -> "Firebase не настроен в приложении."
        else -> "Не удалось связаться с сервером. Проверьте подключение и попробуйте ещё раз."
    }
}

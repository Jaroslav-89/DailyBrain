package com.jaroapps.dailybrain.feature.profile.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jaroapps.dailybrain.feature.profile.domain.model.PublicUserProfile
import com.jaroapps.dailybrain.feature.profile.domain.model.UserProfile
import com.jaroapps.dailybrain.feature.profile.domain.model.UsernamePolicy
import com.jaroapps.dailybrain.feature.profile.domain.repository.FirebaseSessionMismatchException
import com.jaroapps.dailybrain.feature.profile.domain.repository.UserDirectoryNotConfiguredException
import com.jaroapps.dailybrain.feature.profile.domain.repository.UserDirectoryRepository
import com.jaroapps.dailybrain.feature.profile.domain.repository.UserProfileRepository
import com.jaroapps.dailybrain.feature.profile.domain.repository.UsernameTakenException
import com.jaroapps.dailybrain.feature.profile.domain.usecase.ObserveUserProfileUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class UsernameAvailability { UNKNOWN, AVAILABLE, TAKEN }

data class UserDirectoryUiState(
    val profile: UserProfile? = null,
    val isCheckingUsername: Boolean = false,
    val usernameAvailability: UsernameAvailability = UsernameAvailability.UNKNOWN,
    val checkedUsername: String? = null,
    val usernameMessage: String? = null,
    val usernameError: Boolean = false,
    val isSavingUsername: Boolean = false,
    val isSearching: Boolean = false,
    val searchHasRun: Boolean = false,
    val searchResult: PublicUserProfile? = null,
    val searchError: String? = null,
)

private data class UserDirectoryActionState(
    val isCheckingUsername: Boolean = false,
    val usernameAvailability: UsernameAvailability = UsernameAvailability.UNKNOWN,
    val checkedUsername: String? = null,
    val usernameMessage: String? = null,
    val usernameError: Boolean = false,
    val isSavingUsername: Boolean = false,
    val isSearching: Boolean = false,
    val searchHasRun: Boolean = false,
    val searchResult: PublicUserProfile? = null,
    val searchError: String? = null,
)

class UserDirectoryViewModel(
    observeUserProfile: ObserveUserProfileUseCase,
    private val userDirectoryRepository: UserDirectoryRepository,
    private val userProfileRepository: UserProfileRepository,
) : ViewModel() {
    private val profile: StateFlow<UserProfile?> = observeUserProfile()
        .map<UserProfile, UserProfile?> { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val actions = MutableStateFlow(UserDirectoryActionState())

    val uiState: StateFlow<UserDirectoryUiState> = combine(profile, actions) { activeProfile, action ->
        UserDirectoryUiState(
            profile = activeProfile,
            isCheckingUsername = action.isCheckingUsername,
            usernameAvailability = action.usernameAvailability,
            checkedUsername = action.checkedUsername,
            usernameMessage = action.usernameMessage,
            usernameError = action.usernameError,
            isSavingUsername = action.isSavingUsername,
            isSearching = action.isSearching,
            searchHasRun = action.searchHasRun,
            searchResult = action.searchResult,
            searchError = action.searchError,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserDirectoryUiState())

    fun resetUsernameCheck() {
        actions.value = actions.value.copy(
            isCheckingUsername = false,
            usernameAvailability = UsernameAvailability.UNKNOWN,
            checkedUsername = null,
            usernameMessage = null,
            usernameError = false,
        )
    }

    fun checkUsernameAvailability(input: String) {
        val activeProfile = profile.value
        val firebaseUid = activeProfile?.firebaseUid
        if (activeProfile == null || firebaseUid == null) {
            actions.value = actions.value.copy(
                usernameMessage = "Сначала подключите Google-аккаунт.",
                usernameError = true,
            )
            return
        }

        val normalized = UsernamePolicy.normalize(input)
        if (!UsernamePolicy.isValid(normalized)) {
            actions.value = actions.value.copy(
                usernameAvailability = UsernameAvailability.UNKNOWN,
                checkedUsername = null,
                usernameMessage = "Используйте 3–20 латинских букв, цифр или символов подчёркивания.",
                usernameError = true,
            )
            return
        }
        if (normalized == activeProfile.username.orEmpty()) {
            actions.value = actions.value.copy(
                usernameAvailability = UsernameAvailability.AVAILABLE,
                checkedUsername = normalized,
                usernameMessage = "Это ваш текущий ник.",
                usernameError = false,
            )
            return
        }

        actions.value = actions.value.copy(
            isCheckingUsername = true,
            usernameAvailability = UsernameAvailability.UNKNOWN,
            checkedUsername = null,
            usernameMessage = null,
            usernameError = false,
        )
        viewModelScope.launch {
            try {
                val available = userDirectoryRepository.isUsernameAvailable(firebaseUid, normalized)
                actions.value = actions.value.copy(
                    isCheckingUsername = false,
                    usernameAvailability = if (available) UsernameAvailability.AVAILABLE else UsernameAvailability.TAKEN,
                    checkedUsername = normalized,
                    usernameMessage = if (available) "Ник свободен. Теперь его можно сохранить."
                    else "Этот ник уже занят.",
                    usernameError = !available,
                )
            } catch (error: Exception) {
                actions.value = actions.value.copy(
                    isCheckingUsername = false,
                    usernameAvailability = UsernameAvailability.UNKNOWN,
                    checkedUsername = null,
                    usernameMessage = errorMessage(error),
                    usernameError = true,
                )
            }
        }
    }

    fun saveUsername(input: String) {
        val activeProfile = profile.value
        val firebaseUid = activeProfile?.firebaseUid
        if (activeProfile == null || firebaseUid == null) {
            actions.value = actions.value.copy(
                usernameMessage = "Сначала подключите Google-аккаунт.",
                usernameError = true,
            )
            return
        }

        val normalized = UsernamePolicy.normalize(input)
        if (normalized.isNotEmpty() && !UsernamePolicy.isValid(normalized)) {
            actions.value = actions.value.copy(
                usernameMessage = "Используйте 3–20 латинских букв, цифр или символов подчёркивания.",
                usernameError = true,
            )
            return
        }
        if (normalized == activeProfile.username.orEmpty()) return
        if (normalized.isNotEmpty() &&
            (actions.value.checkedUsername != normalized ||
                actions.value.usernameAvailability != UsernameAvailability.AVAILABLE)
        ) {
            actions.value = actions.value.copy(
                usernameMessage = "Сначала проверьте доступность нового ника.",
                usernameError = true,
            )
            return
        }

        actions.value = actions.value.copy(isSavingUsername = true, usernameMessage = null, usernameError = false)
        viewModelScope.launch {
            try {
                // The transaction rechecks uniqueness; the earlier UI check is advisory only.
                userDirectoryRepository.updateUsername(firebaseUid, normalized)
                userProfileRepository.updateUsername(
                    localAccountId = activeProfile.localAccountId,
                    username = normalized.takeIf(String::isNotBlank),
                )
                actions.value = actions.value.copy(
                    isSavingUsername = false,
                    usernameAvailability = UsernameAvailability.UNKNOWN,
                    checkedUsername = null,
                    usernameMessage = if (normalized.isBlank()) "Ник удалён." else "Ник сохранён.",
                    usernameError = false,
                )
            } catch (error: Exception) {
                actions.value = actions.value.copy(
                    isSavingUsername = false,
                    usernameMessage = errorMessage(error),
                    usernameError = true,
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
        is UsernameTakenException -> "Этот ник уже занят. Выберите другой."
        is FirebaseSessionMismatchException -> "Активный Google-аккаунт не совпадает с выбранным профилем. Повторно подключите нужный аккаунт."
        is UserDirectoryNotConfiguredException -> "Firebase не настроен в приложении."
        else -> "Не удалось связаться с сервером. Проверьте подключение и попробуйте ещё раз."
    }
}

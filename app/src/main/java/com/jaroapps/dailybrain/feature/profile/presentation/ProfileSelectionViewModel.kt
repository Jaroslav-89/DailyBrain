package com.jaroapps.dailybrain.feature.profile.presentation

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CancellationException
import androidx.lifecycle.viewModelScope
import com.jaroapps.dailybrain.feature.profile.domain.model.UserProfile
import com.jaroapps.dailybrain.feature.profile.domain.usecase.CreateLocalProfileUseCase
import com.jaroapps.dailybrain.feature.profile.domain.usecase.DeleteLocalProfileUseCase
import com.jaroapps.dailybrain.feature.profile.domain.usecase.ObserveLocalProfilesUseCase
import com.jaroapps.dailybrain.feature.profile.domain.usecase.SwitchActiveProfileUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProfileSelectionUiState(
    val profiles: List<UserProfile> = emptyList(),
    val isLoading: Boolean = true,
    val isWorking: Boolean = false,
    val error: String? = null,
)

class ProfileSelectionViewModel(
    observeLocalProfiles: ObserveLocalProfilesUseCase,
    private val createLocalProfile: CreateLocalProfileUseCase,
    private val switchActiveProfile: SwitchActiveProfileUseCase,
    private val deleteLocalProfile: DeleteLocalProfileUseCase,
) : ViewModel() {
    private val profilesLoaded = MutableStateFlow(false)
    private val profiles = observeLocalProfiles()
        .onEach { profilesLoaded.value = true }
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            emptyList(),
        )
    private val isWorking = MutableStateFlow(false)
    private val error = MutableStateFlow<String?>(null)

    val uiState: StateFlow<ProfileSelectionUiState> = combine(
        profiles,
        profilesLoaded,
        isWorking,
        error,
    ) { availableProfiles, loaded, working, actionError ->
        ProfileSelectionUiState(
            profiles = availableProfiles,
            isLoading = !loaded,
            isWorking = working,
            error = actionError,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        ProfileSelectionUiState(),
    )

    fun selectProfile(localAccountId: String, onSelected: () -> Unit) {
        viewModelScope.launch {
            runAction {
                switchActiveProfile(localAccountId)
                onSelected()
            }
        }
    }

    fun createProfile(onCreated: (UserProfile) -> Unit) {
        viewModelScope.launch {
            runAction {
                val profile = createLocalProfile()
                switchActiveProfile(profile.localAccountId)
                onCreated(profile)
            }
        }
    }

    fun deleteProfile(localAccountId: String) {
        viewModelScope.launch {
            runAction {
                deleteLocalProfile(localAccountId)
            }
        }
    }

    private fun profileActionError(error: Exception): String =
        "Не удалось выполнить действие с профилем. Попробуйте ещё раз."

    private suspend fun runAction(action: suspend () -> Unit) {
        if (isWorking.value) return
        isWorking.value = true
        error.value = null
        try {
            action()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            this@ProfileSelectionViewModel.error.value = profileActionError(error)
        } finally {
            isWorking.value = false
        }
    }
}

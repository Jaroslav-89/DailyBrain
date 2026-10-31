package com.jaroapps.dailybrain.feature.profile.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jaroapps.dailybrain.feature.profile.data.auth.GoogleSignInResult
import com.jaroapps.dailybrain.feature.profile.domain.model.LinkGoogleAccountResult
import com.jaroapps.dailybrain.feature.profile.domain.model.UserProfile
import com.jaroapps.dailybrain.feature.profile.domain.usecase.ObserveProfileStatisticsUseCase
import com.jaroapps.dailybrain.feature.profile.domain.usecase.ObserveUserProfileUseCase
import com.jaroapps.dailybrain.feature.profile.domain.repository.ProfileStatistics
import com.jaroapps.dailybrain.feature.profile.domain.usecase.SaveUserProfileUseCase
import com.jaroapps.dailybrain.feature.profile.domain.usecase.ObserveLocalProfilesUseCase
import com.jaroapps.dailybrain.feature.profile.domain.usecase.CreateLocalProfileUseCase
import com.jaroapps.dailybrain.feature.profile.domain.usecase.SwitchActiveProfileUseCase
import com.jaroapps.dailybrain.feature.profile.domain.usecase.DeleteActiveProfileUseCase
import com.jaroapps.dailybrain.feature.profile.domain.usecase.LinkGoogleAccountUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class GoogleLinkError {
    ALREADY_LINKED_TO_ANOTHER_PROFILE,
    FIREBASE_NOT_CONFIGURED,
    FIREBASE_AUTH_FAILED,
    SAVE_FAILED,
}

private data class ProfileActionState(
    val saveError: Boolean,
    val deleteError: Boolean,
    val isGoogleLinking: Boolean,
    val googleLinkError: GoogleLinkError?,
)

data class ProfileUiState(
    val profile: UserProfile? = null,
    val profiles: List<UserProfile> = emptyList(),
    val statistics: ProfileStatistics = ProfileStatistics(),
    val isSaving: Boolean = false,
    val saveError: Boolean = false,
    val deleteError: Boolean = false,
    val isGoogleLinking: Boolean = false,
    val googleLinkError: GoogleLinkError? = null,
)

class ProfileViewModel(
    observeUserProfile: ObserveUserProfileUseCase,
    observeProfileStatistics: ObserveProfileStatisticsUseCase,
    private val saveUserProfile: SaveUserProfileUseCase,
    observeLocalProfiles: ObserveLocalProfilesUseCase,
    private val createLocalProfile: CreateLocalProfileUseCase,
    private val switchActiveProfile: SwitchActiveProfileUseCase,
    private val deleteActiveProfile: DeleteActiveProfileUseCase,
    private val linkGoogleAccountUseCase: LinkGoogleAccountUseCase,
) : ViewModel() {
    private val saving = MutableStateFlow(false)
    private val saveError = MutableStateFlow(false)
    private val deleteError = MutableStateFlow(false)
    private val googleLinking = MutableStateFlow(false)
    private val googleLinkError = MutableStateFlow<GoogleLinkError?>(null)

    private val actionState = combine(
        saveError,
        deleteError,
        googleLinking,
        googleLinkError,
    ) { hasSaveError, hasDeleteError, isLinking, linkError ->
        ProfileActionState(hasSaveError, hasDeleteError, isLinking, linkError)
    }

    val uiState: StateFlow<ProfileUiState> = combine(
        observeUserProfile(),
        observeLocalProfiles(),
        observeProfileStatistics(),
        saving,
        actionState,
    ) { profile, profiles, statistics, isSaving, actions ->
        ProfileUiState(
            profile = profile,
            profiles = profiles,
            statistics = statistics,
            isSaving = isSaving,
            saveError = actions.saveError,
            deleteError = actions.deleteError,
            isGoogleLinking = actions.isGoogleLinking,
            googleLinkError = actions.googleLinkError,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileUiState())

    fun createProfile(onProfileChanged: () -> Unit = {}) {
        viewModelScope.launch {
            saveError.value = false
            try {
                val profile = createLocalProfile()
                switchActiveProfile(profile.localAccountId)
                onProfileChanged()
            } catch (_: Exception) {
                saveError.value = true
            }
        }
    }

    fun switchProfile(localAccountId: String, onProfileChanged: () -> Unit = {}) {
        viewModelScope.launch {
            saveError.value = false
            try {
                switchActiveProfile(localAccountId)
                onProfileChanged()
            } catch (_: Exception) {
                saveError.value = true
            }
        }
    }

    fun deleteProfile(onDeleted: () -> Unit) {
        viewModelScope.launch {
            deleteError.value = false
            try {
                deleteActiveProfile()
                onDeleted()
            } catch (_: Exception) {
                deleteError.value = true
            }
        }
    }

    fun clearDeleteError() {
        deleteError.value = false
    }

    fun clearGoogleLinkError() {
        googleLinkError.value = null
    }

    fun linkGoogleAccount(signInResult: GoogleSignInResult) {
        val localAccountId = uiState.value.profile?.localAccountId ?: return
        viewModelScope.launch {
            googleLinking.value = true
            googleLinkError.value = null
            try {
                when (linkGoogleAccountUseCase(localAccountId, signInResult.account, signInResult.idToken)) {
                    LinkGoogleAccountResult.LINKED -> Unit
                    LinkGoogleAccountResult.ALREADY_LINKED_TO_ANOTHER_PROFILE -> {
                        googleLinkError.value = GoogleLinkError.ALREADY_LINKED_TO_ANOTHER_PROFILE
                    }
                    LinkGoogleAccountResult.FIREBASE_NOT_CONFIGURED -> {
                        googleLinkError.value = GoogleLinkError.FIREBASE_NOT_CONFIGURED
                    }
                    LinkGoogleAccountResult.FIREBASE_AUTH_FAILED -> {
                        googleLinkError.value = GoogleLinkError.FIREBASE_AUTH_FAILED
                    }
                    LinkGoogleAccountResult.SAVE_FAILED -> {
                        googleLinkError.value = GoogleLinkError.SAVE_FAILED
                    }
                }
            } catch (_: Exception) {
                googleLinkError.value = GoogleLinkError.SAVE_FAILED
            } finally {
                googleLinking.value = false
            }
        }
    }

    fun saveProfile(
        displayName: String,
        gender: String?,
        birthDate: String,
        country: String,
        city: String,
        onSaved: () -> Unit,
    ) {
        viewModelScope.launch {
            saving.value = true
            saveError.value = false
            try {
                saveUserProfile(
                    displayName = displayName,
                    gender = gender,
                    birthDate = birthDate,
                    country = country,
                    city = city,
                )
                onSaved()
            } catch (_: IllegalArgumentException) {
                saveError.value = true
            } catch (_: Exception) {
                saveError.value = true
            } finally {
                saving.value = false
            }
        }
    }
}
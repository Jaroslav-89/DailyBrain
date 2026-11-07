package com.jaroapps.dailybrain.feature.profile.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jaroapps.dailybrain.feature.profile.data.auth.GoogleSignInResult
import com.jaroapps.dailybrain.feature.profile.domain.model.LinkGoogleAccountResult
import com.jaroapps.dailybrain.feature.profile.domain.model.ProfileCloudSessionStatus
import com.jaroapps.dailybrain.feature.profile.domain.model.UserProfile
import com.jaroapps.dailybrain.feature.profile.domain.usecase.ObserveProfileStatisticsUseCase
import com.jaroapps.dailybrain.feature.profile.domain.usecase.ObserveUserProfileUseCase
import com.jaroapps.dailybrain.feature.profile.domain.repository.ProfileStatistics
import com.jaroapps.dailybrain.feature.profile.domain.repository.FirebaseAuthenticationRepository
import com.jaroapps.dailybrain.feature.profile.domain.repository.ProfileGoogleSignInCancelledException
import com.jaroapps.dailybrain.feature.profile.domain.repository.ProfileGoogleAccountMismatchException
import com.jaroapps.dailybrain.feature.profile.domain.repository.ProfileGoogleAccountNotLinkedException
import com.jaroapps.dailybrain.feature.profile.domain.repository.ProfileFirebaseUidMismatchException
import com.jaroapps.dailybrain.feature.profile.domain.repository.ProfileFirebaseNotConfiguredException
import com.jaroapps.dailybrain.feature.profile.domain.repository.ProfileFirebaseAuthenticationException
import com.jaroapps.dailybrain.feature.profile.domain.model.ProfileCollectible
import com.jaroapps.dailybrain.feature.profile.domain.repository.ProfileCollectiblesRepository
import com.jaroapps.dailybrain.feature.shop.domain.repository.GiftDeliveryRepository
import com.jaroapps.dailybrain.feature.profile.domain.usecase.SaveUserProfileUseCase
import com.jaroapps.dailybrain.feature.profile.domain.usecase.ObserveLocalProfilesUseCase
import com.jaroapps.dailybrain.feature.profile.domain.usecase.CreateLocalProfileUseCase
import com.jaroapps.dailybrain.feature.profile.domain.usecase.SwitchActiveProfileUseCase
import com.jaroapps.dailybrain.feature.profile.domain.usecase.DeleteActiveProfileUseCase
import com.jaroapps.dailybrain.feature.profile.domain.usecase.LinkGoogleAccountUseCase
import com.jaroapps.dailybrain.feature.profile.domain.usecase.ReauthenticateGoogleAccountUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.CancellationException
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
    val googleLinkDiagnostic: String?,
    val isGoogleReauthenticating: Boolean,
    val googleReauthenticationError: String?,
)

private data class ProfileSessionSnapshot(
    val profile: UserProfile,
    val firebaseUid: String?,
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
    val googleLinkDiagnostic: String? = null,
    val cloudSessionStatus: ProfileCloudSessionStatus = ProfileCloudSessionStatus.NOT_LINKED,
    val isGoogleReauthenticating: Boolean = false,
    val googleReauthenticationError: String? = null,
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
    private val reauthenticateGoogleAccountUseCase: ReauthenticateGoogleAccountUseCase,
    private val firebaseAuthenticationRepository: FirebaseAuthenticationRepository,
    profileCollectiblesRepository: ProfileCollectiblesRepository,
    private val giftDeliveryRepository: GiftDeliveryRepository,
) : ViewModel() {
    private val saving = MutableStateFlow(false)
    private val saveError = MutableStateFlow(false)
    private val deleteError = MutableStateFlow(false)
    private val googleLinking = MutableStateFlow(false)
    private val googleLinkError = MutableStateFlow<GoogleLinkError?>(null)
    private val googleLinkDiagnostic = MutableStateFlow<String?>(null)
    private val googleReauthenticating = MutableStateFlow(false)
    private val googleReauthenticationError = MutableStateFlow<String?>(null)

    private val baseActionState = combine(
        saveError,
        deleteError,
        googleLinking,
        googleLinkError,
        googleLinkDiagnostic,
    ) { hasSaveError, hasDeleteError, isLinking, linkError, diagnostic ->
        ProfileActionState(
            saveError = hasSaveError,
            deleteError = hasDeleteError,
            isGoogleLinking = isLinking,
            googleLinkError = linkError,
            googleLinkDiagnostic = diagnostic,
            isGoogleReauthenticating = false,
            googleReauthenticationError = null,
        )
    }
    private val actionState = combine(
        baseActionState,
        googleReauthenticating,
        googleReauthenticationError,
    ) { base, isReauthenticating, reauthError ->
        base.copy(
            isGoogleReauthenticating = isReauthenticating,
            googleReauthenticationError = reauthError,
        )
    }

    private val profileSession = combine(
        observeUserProfile(),
        firebaseAuthenticationRepository.observeCurrentUserId(),
    ) { profile, firebaseUid ->
        ProfileSessionSnapshot(profile, firebaseUid)
    }

    val collectibles: StateFlow<List<ProfileCollectible>> = profileCollectiblesRepository.observeOwn()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun refreshIncomingGifts() {
        viewModelScope.launch { runCatching { giftDeliveryRepository.syncIncoming() } }
    }

    val uiState: StateFlow<ProfileUiState> = combine(
        profileSession,
        observeLocalProfiles(),
        observeProfileStatistics(),
        saving,
        actionState,
    ) { session, profiles, statistics, isSaving, actions ->
        val cloudSessionStatus = when {
            session.profile.firebaseUid.isNullOrBlank() -> ProfileCloudSessionStatus.NOT_LINKED
            session.firebaseUid == session.profile.firebaseUid -> ProfileCloudSessionStatus.READY
            else -> ProfileCloudSessionStatus.REAUTH_REQUIRED
        }
        ProfileUiState(
            profile = session.profile,
            profiles = profiles,
            statistics = statistics,
            isSaving = isSaving,
            saveError = actions.saveError,
            deleteError = actions.deleteError,
            isGoogleLinking = actions.isGoogleLinking,
            googleLinkError = actions.googleLinkError,
            googleLinkDiagnostic = actions.googleLinkDiagnostic,
            cloudSessionStatus = cloudSessionStatus,
            isGoogleReauthenticating = actions.isGoogleReauthenticating,
            googleReauthenticationError = actions.googleReauthenticationError,
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
        googleLinkDiagnostic.value = null
    }

    fun clearGoogleReauthenticationError() {
        googleReauthenticationError.value = null
    }

    fun reauthenticateGoogleAccount() {
        val localAccountId = uiState.value.profile?.localAccountId ?: return
        if (googleReauthenticating.value) return
        viewModelScope.launch {
            googleReauthenticating.value = true
            googleReauthenticationError.value = null
            try {
                reauthenticateGoogleAccountUseCase(localAccountId)
                refreshIncomingGifts()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                googleReauthenticationError.value = when (error) {
                    is ProfileGoogleSignInCancelledException ->
                        "Вход в Google отменён. Облачные функции остаются отключёнными."
                    is ProfileGoogleAccountMismatchException ->
                        "Выбран другой Google-аккаунт. Выберите аккаунт, привязанный к этому профилю."
                    is ProfileGoogleAccountNotLinkedException ->
                        "У профиля нет привязки Google. Сначала подключите аккаунт."
                    is ProfileFirebaseUidMismatchException ->
                        "Firebase вернул другой аккаунт. Сохранённая привязка не изменена."
                    is ProfileFirebaseNotConfiguredException ->
                        "Firebase не настроен в приложении. Проверьте app/google-services.json."
                    is ProfileFirebaseAuthenticationException ->
                        firebaseDiagnosticDescription(error.diagnosticCode)
                    else -> "Не удалось восстановить подключение Google. Попробуйте ещё раз."
                }
            } finally {
                googleReauthenticating.value = false
            }
        }
    }

    fun linkGoogleAccount(signInResult: GoogleSignInResult) {
        val localAccountId = uiState.value.profile?.localAccountId ?: return
        viewModelScope.launch {
            googleLinking.value = true
            googleLinkError.value = null
            googleLinkDiagnostic.value = null
            try {
                when (val result = linkGoogleAccountUseCase(localAccountId, signInResult.account, signInResult.idToken)) {
                    LinkGoogleAccountResult.LINKED -> Unit
                    LinkGoogleAccountResult.ALREADY_LINKED_TO_ANOTHER_PROFILE -> {
                        googleLinkError.value = GoogleLinkError.ALREADY_LINKED_TO_ANOTHER_PROFILE
                    }
                    LinkGoogleAccountResult.FIREBASE_NOT_CONFIGURED -> {
                        googleLinkError.value = GoogleLinkError.FIREBASE_NOT_CONFIGURED
                    }
                    is LinkGoogleAccountResult.FIREBASE_AUTH_FAILED -> {
                        googleLinkError.value = GoogleLinkError.FIREBASE_AUTH_FAILED
                        googleLinkDiagnostic.value = result.diagnosticCode
                    }
                    LinkGoogleAccountResult.SAVE_FAILED -> {
                        googleLinkError.value = GoogleLinkError.SAVE_FAILED
                        googleLinkDiagnostic.value = "LOCAL_PROFILE_SAVE_FAILED"
                    }
                }
            } catch (_: Exception) {
                googleLinkError.value = GoogleLinkError.SAVE_FAILED
                googleLinkDiagnostic.value = "LOCAL_PROFILE_LINK_FAILED"
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
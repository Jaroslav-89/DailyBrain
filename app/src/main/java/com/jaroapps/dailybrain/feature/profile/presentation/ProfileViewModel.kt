package com.jaroapps.dailybrain.feature.profile.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jaroapps.dailybrain.feature.profile.domain.model.UserProfile
import com.jaroapps.dailybrain.feature.profile.domain.usecase.ObserveProfileStatisticsUseCase
import com.jaroapps.dailybrain.feature.profile.domain.usecase.ObserveUserProfileUseCase
import com.jaroapps.dailybrain.feature.profile.domain.repository.ProfileStatistics
import com.jaroapps.dailybrain.feature.profile.domain.usecase.SaveUserProfileUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProfileUiState(
    val profile: UserProfile? = null,
    val statistics: ProfileStatistics = ProfileStatistics(),
    val isSaving: Boolean = false,
    val saveError: Boolean = false,
)

class ProfileViewModel(
    observeUserProfile: ObserveUserProfileUseCase,
    observeProfileStatistics: ObserveProfileStatisticsUseCase,
    private val saveUserProfile: SaveUserProfileUseCase,
) : ViewModel() {
    private val saving = MutableStateFlow(false)
    private val saveError = MutableStateFlow(false)

    val uiState: StateFlow<ProfileUiState> = combine(
        observeUserProfile(),
        observeProfileStatistics(),
        saving,
        saveError,
    ) { profile, statistics, isSaving, hasError ->
        ProfileUiState(profile, statistics, isSaving, hasError)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileUiState())

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

package com.jaroapps.dailybrain.feature.profile.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jaroapps.dailybrain.feature.profile.domain.model.ProfileCollectible
import com.jaroapps.dailybrain.feature.profile.domain.model.ProfileCollectibleKind
import com.jaroapps.dailybrain.feature.profile.domain.repository.ProfileCollectiblesRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ProfileCollectiblesUiState(
    val items: List<ProfileCollectible> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
)

class ProfileCollectiblesViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: ProfileCollectiblesRepository,
) : ViewModel() {
    val ownerUid: String = savedStateHandle.get<String>("ownerUid") ?: "local"
    val kind: ProfileCollectibleKind =
        ProfileCollectibleKind.entries.firstOrNull { it.name == savedStateHandle.get<String>("kind") }
            ?: ProfileCollectibleKind.GIFT

    private val _uiState = MutableStateFlow(ProfileCollectiblesUiState())
    val uiState: StateFlow<ProfileCollectiblesUiState> = _uiState

    init {
        viewModelScope.launch {
            try {
                val items = repository.getForOwner(ownerUid)
                    .filter { it.kind == kind }
                    .sortedByDescending(ProfileCollectible::acquiredAtEpochMs)
                _uiState.value = ProfileCollectiblesUiState(items = items, isLoading = false)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _uiState.value = ProfileCollectiblesUiState(
                    isLoading = false,
                    error = error.message ?: "Не удалось загрузить предметы.",
                )
            }
        }
    }
}

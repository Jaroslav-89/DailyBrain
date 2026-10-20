package com.jaroapps.dailybrain.feature.avatar.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jaroapps.dailybrain.feature.avatar.domain.model.AvatarConfiguration
import com.jaroapps.dailybrain.feature.avatar.domain.usecase.ObserveAvatarUseCase
import com.jaroapps.dailybrain.feature.avatar.domain.usecase.ResetAvatarUseCase
import com.jaroapps.dailybrain.feature.avatar.domain.usecase.SaveAvatarUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AvatarEditorUiState(
    val configuration: AvatarConfiguration = AvatarConfiguration.Default,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val saveError: Boolean = false,
)

class AvatarViewModel(
    observeAvatar: ObserveAvatarUseCase,
    private val saveAvatar: SaveAvatarUseCase,
    private val resetAvatar: ResetAvatarUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AvatarEditorUiState())
    val uiState: StateFlow<AvatarEditorUiState> = _uiState.asStateFlow()
    private var hasLocalDraft = false

    init {
        viewModelScope.launch {
            observeAvatar().collect { stored ->
                val current = _uiState.value
                _uiState.value = if (hasLocalDraft) {
                    current.copy(isLoading = false)
                } else {
                    current.copy(configuration = stored, isLoading = false)
                }
            }
        }
    }

    fun updateConfiguration(configuration: AvatarConfiguration) {
        hasLocalDraft = true
        _uiState.value = _uiState.value.copy(configuration = configuration, saveError = false)
    }

    fun save(onSaved: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, saveError = false)
            try {
                saveAvatar(_uiState.value.configuration)
                hasLocalDraft = false
                onSaved()
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(saveError = true)
            } finally {
                _uiState.value = _uiState.value.copy(isSaving = false)
            }
        }
    }

    fun reset() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, saveError = false)
            try {
                resetAvatar()
                hasLocalDraft = false
                _uiState.value = _uiState.value.copy(configuration = AvatarConfiguration.Default)
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(saveError = true)
            } finally {
                _uiState.value = _uiState.value.copy(isSaving = false)
            }
        }
    }
}

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

data class CollectibleDetailUiState(
    val item: ProfileCollectible? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
)

class CollectibleDetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: ProfileCollectiblesRepository,
) : ViewModel() {
    private val ownerUid: String = savedStateHandle["ownerUid"] ?: "local"
    private val itemId: String = savedStateHandle["itemId"].orEmpty()
    private val kind: ProfileCollectibleKind =
        ProfileCollectibleKind.entries.firstOrNull { it.name == savedStateHandle.get<String>("kind") }
            ?: ProfileCollectibleKind.GIFT

    private val _uiState = MutableStateFlow(CollectibleDetailUiState())
    val uiState: StateFlow<CollectibleDetailUiState> = _uiState

    init {
        viewModelScope.launch {
            try {
                _uiState.value = CollectibleDetailUiState(
                    item = repository.getItem(ownerUid, itemId, kind),
                    isLoading = false,
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _uiState.value = CollectibleDetailUiState(
                    isLoading = false,
                    error = error.message ?: "Не удалось загрузить предмет.",
                )
            }
        }
    }
}

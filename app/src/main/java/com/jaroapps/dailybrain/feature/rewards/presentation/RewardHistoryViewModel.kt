package com.jaroapps.dailybrain.feature.rewards.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jaroapps.dailybrain.feature.rewards.domain.model.RewardBalanceType
import com.jaroapps.dailybrain.feature.rewards.domain.model.RewardHistoryEntry
import com.jaroapps.dailybrain.feature.rewards.domain.repository.RewardHistoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class RewardHistoryUiState(
    val balanceType: RewardBalanceType,
    val balance: Int = 0,
    val entries: List<RewardHistoryEntry> = emptyList(),
    val isLoading: Boolean = true,
)

class RewardHistoryViewModel(
    savedStateHandle: SavedStateHandle,
    repository: RewardHistoryRepository,
) : ViewModel() {
    private val selectedType = MutableStateFlow(
        RewardBalanceType.fromRouteValue(savedStateHandle["currency"]),
    )

    val uiState: StateFlow<RewardHistoryUiState> = selectedType
        .flatMapLatest { type ->
            repository.observeHistory(type).map { snapshot ->
                RewardHistoryUiState(
                    balanceType = type,
                    balance = snapshot.balance,
                    entries = snapshot.entries,
                    isLoading = false,
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = RewardHistoryUiState(balanceType = selectedType.value),
        )

    fun selectBalanceType(type: RewardBalanceType) {
        selectedType.value = type
    }
}

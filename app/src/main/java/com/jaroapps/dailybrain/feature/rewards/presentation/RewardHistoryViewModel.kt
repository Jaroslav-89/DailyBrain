package com.jaroapps.dailybrain.feature.rewards.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jaroapps.dailybrain.feature.rewards.domain.model.RewardBalanceType
import com.jaroapps.dailybrain.feature.rewards.domain.model.RewardHistoryWeekSection
import com.jaroapps.dailybrain.feature.rewards.domain.model.groupRewardHistoryByWeek
import com.jaroapps.dailybrain.feature.rewards.domain.repository.RewardHistoryRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.ZoneId

data class RewardHistoryUiState(
    val balanceType: RewardBalanceType,
    val balance: Int = 0,
    val weekSections: List<RewardHistoryWeekSection> = emptyList(),
    val isLoading: Boolean = true,
)

class RewardHistoryViewModel(
    savedStateHandle: SavedStateHandle,
    repository: RewardHistoryRepository,
) : ViewModel() {
    private val balanceType = RewardBalanceType.fromRouteValue(savedStateHandle["currency"])

    val uiState: StateFlow<RewardHistoryUiState> = repository.observeHistory(balanceType)
        .map { snapshot ->
            RewardHistoryUiState(
                balanceType = balanceType,
                balance = snapshot.balance,
                weekSections = groupRewardHistoryByWeek(
                    entries = snapshot.entries,
                    zoneId = ZoneId.systemDefault(),
                ),
                isLoading = false,
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = RewardHistoryUiState(balanceType = balanceType),
        )
}

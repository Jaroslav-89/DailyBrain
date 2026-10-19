package com.jaroapps.dailybrain.feature.counting.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jaroapps.dailybrain.feature.counting.domain.model.CountingStatistics
import com.jaroapps.dailybrain.feature.counting.domain.usecase.ObserveCountingStatisticsUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class CountingStatisticsViewModel(
    observeCountingStatistics: ObserveCountingStatisticsUseCase,
) : ViewModel() {

    val uiState = observeCountingStatistics()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = CountingStatistics(),
        )
}

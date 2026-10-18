package com.jaroapps.dailybrain.feature.counting.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jaroapps.dailybrain.feature.counting.domain.model.CountingStatistics
import com.jaroapps.dailybrain.feature.counting.domain.repository.CountingTrainingRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class CountingStatisticsViewModel(
    repository: CountingTrainingRepository,
) : ViewModel() {

    val uiState = repository.observeStatistics()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = CountingStatistics(),
        )
}
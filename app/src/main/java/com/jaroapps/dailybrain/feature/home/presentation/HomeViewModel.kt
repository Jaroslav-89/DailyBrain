package com.jaroapps.dailybrain.feature.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jaroapps.dailybrain.common.domain.usecase.ObserveAllCompletedTrainingsUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class HomeViewModel(
    observeAllCompletedTrainings: ObserveAllCompletedTrainingsUseCase,
) : ViewModel() {

    private val _uiState: StateFlow<HomeUiState> = observeAllCompletedTrainings()
        .map { sessions ->
            val zone = ZoneId.systemDefault()
            val completedDates = sessions.map { session ->
                Instant.ofEpochMilli(session.finishedAtEpochMs).atZone(zone).toLocalDate()
            }.toSet()

            HomeUiState(
                today = LocalDate.now(zone),
                completedDates = completedDates,
                totalTrainingCount = sessions.size,
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState(),
        )


    val uiState: StateFlow<HomeUiState> = _uiState
}

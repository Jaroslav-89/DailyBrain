package com.jaroapps.dailybrain.feature.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jaroapps.dailybrain.feature.history.domain.repository.TrainingSessionRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class HomeViewModel(
    repository: TrainingSessionRepository,
) : ViewModel() {

    val uiState = repository.observeAll()
        .map { sessions ->
            val zone = ZoneId.systemDefault()

            val completedDates = sessions
                .map { session ->
                    Instant.ofEpochMilli(session.finishedAtEpochMs)
                        .atZone(zone)
                        .toLocalDate()
                }
                .toSet()

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
}

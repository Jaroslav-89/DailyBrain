package com.jaroapps.dailybrain.feature.statistics.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jaroapps.dailybrain.feature.history.domain.repository.HistoryRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class StatisticsViewModel(
    repository: HistoryRepository,
) : ViewModel() {

    val uiState = repository.observeAll()
        .map { sessions ->
            val zone = ZoneId.systemDefault()
            val today = LocalDate.now(zone)
            val weekStart = today.minusDays(6)

            val weeklySessions = sessions.filter { session ->
                val date = Instant
                    .ofEpochMilli(session.finishedAtEpochMs)
                    .atZone(zone)
                    .toLocalDate()

                !date.isBefore(weekStart) && !date.isAfter(today)
            }

            StatisticsUiState(
                totalTrainingCount = sessions.size,
                weeklyTrainingCount = weeklySessions.size,
                averageAccuracyPercent = sessions
                    .takeIf { it.isNotEmpty() }
                    ?.map { it.accuracyPercent }
                    ?.average()
                    ?: 0.0,
                averageDurationMs = sessions
                    .takeIf { it.isNotEmpty() }
                    ?.map { it.durationMs }
                    ?.average()
                    ?.toLong()
                    ?: 0L,
                bestAccuracyPercent = sessions
                    .maxOfOrNull { it.accuracyPercent }
                    ?: 0.0,
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = StatisticsUiState(),
        )
}

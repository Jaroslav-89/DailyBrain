package com.jaroapps.dailybrain.feature.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jaroapps.dailybrain.common.domain.model.ExerciseType
import com.jaroapps.dailybrain.common.domain.usecase.ObserveAllCompletedTrainingsUseCase
import com.jaroapps.dailybrain.common.domain.usecase.ObserveTotalTrainingCountUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    observeAllCompletedTrainings: ObserveAllCompletedTrainingsUseCase,
    observeTotalTrainingCount: ObserveTotalTrainingCountUseCase,
) : ViewModel() {
    private val zone = ZoneId.systemDefault()

    private val todayFlow = flow {
        while (true) {
            val today = LocalDate.now(zone)
            emit(today)
            val nextMidnight = today.plusDays(1).atStartOfDay(zone).toInstant()
            delay(Duration.between(Instant.now(), nextMidnight).toMillis().coerceAtLeast(1L))
        }
    }

    val uiState: StateFlow<HomeUiState> = combine(
        todayFlow,
        observeAllCompletedTrainings(),
        observeTotalTrainingCount(),
    ) { today, sessions, totalCount ->
        val completedDates = sessions.mapNotNull { session ->
            runCatching {
                Instant.ofEpochMilli(session.finishedAtEpochMs).atZone(zone).toLocalDate()
            }.getOrNull()
        }.toSet()
        val typedDates = sessions.mapNotNull { session ->
            val date = runCatching {
                Instant.ofEpochMilli(session.finishedAtEpochMs).atZone(zone).toLocalDate()
            }.getOrNull() ?: return@mapNotNull null
            session.exerciseType to date
        }.groupBy({ it.first }, { it.second })
            .mapValues { (_, dates) -> dates.toSet() }

        HomeUiState(
            today = today,
            completedDates = completedDates,
            completedExerciseDatesByType = typedDates,
            totalTrainingCount = totalCount,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(),
    )
}

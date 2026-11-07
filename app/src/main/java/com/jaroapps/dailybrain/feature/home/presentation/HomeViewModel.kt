package com.jaroapps.dailybrain.feature.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jaroapps.dailybrain.common.domain.model.ExerciseType
import com.jaroapps.dailybrain.common.domain.model.TrainingStreakCalculator
import com.jaroapps.dailybrain.common.domain.usecase.ObserveActiveTrainingDatesUseCase
import com.jaroapps.dailybrain.common.domain.usecase.ObservePatchedDatesUseCase
import com.jaroapps.dailybrain.common.domain.usecase.ObserveAllCompletedTrainingsUseCase
import com.jaroapps.dailybrain.common.domain.usecase.ObserveTotalTrainingCountUseCase
import com.jaroapps.dailybrain.common.domain.usecase.InitializeRewardStateUseCase
import com.jaroapps.dailybrain.feature.profile.domain.usecase.ObserveUserProfileUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    observeAllCompletedTrainings: ObserveAllCompletedTrainingsUseCase,
    observeActiveTrainingDates: ObserveActiveTrainingDatesUseCase,
    observePatchedDates: ObservePatchedDatesUseCase,
    observeTotalTrainingCount: ObserveTotalTrainingCountUseCase,
    observeUserProfile: ObserveUserProfileUseCase,
    initializeRewardState: InitializeRewardStateUseCase,
) : ViewModel() {
    private val zone = ZoneId.systemDefault()

    init {
        viewModelScope.launch {
            runCatching { initializeRewardState() }
        }
    }

    private val todayFlow = flow {
        while (true) {
            val today = LocalDate.now(zone)
            emit(today)
            val nextMidnight = today.plusDays(1).atStartOfDay(zone).toInstant()
            delay(Duration.between(Instant.now(), nextMidnight).toMillis().coerceAtLeast(1L))
        }
    }

    private val dateSources = combine(
        todayFlow,
        observeActiveTrainingDates(),
        observePatchedDates(),
    ) { today, activeDateKeys, patchedDateKeys ->
        Triple(today, activeDateKeys, patchedDateKeys)
    }

    val uiState: StateFlow<HomeUiState> = combine(
        dateSources,
        observeAllCompletedTrainings(),
        observeTotalTrainingCount(),
        observeUserProfile(),
    ) { (today, activeDateKeys, patchedDateKeys), sessions, totalCount, profile ->
        val trainingDates = TrainingStreakCalculator.parseDateKeys(activeDateKeys)
        val patchedDates = TrainingStreakCalculator.parseDateKeys(patchedDateKeys)
        val streakDates = trainingDates + patchedDates
        val typedDates = sessions.mapNotNull { session ->
            val date = runCatching {
                Instant.ofEpochMilli(session.finishedAtEpochMs).atZone(zone).toLocalDate()
            }.getOrNull() ?: return@mapNotNull null
            session.exerciseType to date
        }.groupBy({ it.first }, { it.second })
            .mapValues { (_, dates) -> dates.toSet() }

        HomeUiState(
            today = today,
            completedDates = streakDates,
            patchedDates = patchedDates,
            completedExerciseDatesByType = typedDates,
            isTrainingHistoryLoaded = true,
            totalTrainingCount = totalCount,
            xpPoints = profile.xpPoints,
            endorphinBalance = profile.endorphinBalance,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(),
    )
}

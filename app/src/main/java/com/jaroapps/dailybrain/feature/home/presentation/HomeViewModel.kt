package com.jaroapps.dailybrain.feature.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jaroapps.dailybrain.common.domain.usecase.ObserveTotalTrainingCountUseCase
import com.jaroapps.dailybrain.common.domain.usecase.ObserveTrainingDaysInRangeUseCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    observeTrainingDaysInRange: ObserveTrainingDaysInRangeUseCase,
    observeTotalTrainingCount: ObserveTotalTrainingCountUseCase,
) : ViewModel() {

    private val zone = ZoneId.systemDefault()

    private val todayFlow = flow {
        while (true) {
            val today = LocalDate.now(zone)
            emit(today)

            val nextMidnight = today.plusDays(1).atStartOfDay(zone).toInstant()
            val delayMillis = Duration.between(Instant.now(), nextMidnight)
                .toMillis()
                .coerceAtLeast(1L)
            delay(delayMillis)
        }
    }

    private val _uiState: StateFlow<HomeUiState> = todayFlow
        .flatMapLatest { today ->
            val startDate = today.minusDays(6).toDatabaseDate()
            val endDate = today.toDatabaseDate()

            combine(
                observeTrainingDaysInRange(startDate, endDate),
                observeTotalTrainingCount(),
            ) { activeDates, totalCount ->
                HomeUiState(
                    today = today,
                    completedDates = activeDates.map { it.toLocalDate() }.toSet(),
                    totalTrainingCount = totalCount,
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState(),
        )

    val uiState: StateFlow<HomeUiState> = _uiState

    private fun LocalDate.toDatabaseDate(): Int =
        format(DateTimeFormatter.BASIC_ISO_DATE).toInt()

    private fun Int.toLocalDate(): LocalDate =
        LocalDate.parse(toString(), DateTimeFormatter.BASIC_ISO_DATE)
}

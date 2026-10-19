package com.jaroapps.dailybrain.feature.history.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jaroapps.dailybrain.common.domain.usecase.ObserveActiveTrainingDatesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

class CalendarViewModel(
    observeActiveTrainingDates: ObserveActiveTrainingDatesUseCase,
) : ViewModel() {

    private val displayedMonth = MutableStateFlow(YearMonth.now())

    val uiState = combine(
        displayedMonth,
        observeActiveTrainingDates(),
    ) { month, dateKeys ->
        val activeDates = dateKeys.mapNotNull { key ->
            runCatching {
                LocalDate.parse(
                    key.toString(),
                    DateTimeFormatter.BASIC_ISO_DATE,
                )
            }.getOrNull()
        }.toSet()

        CalendarUiState(
            displayedMonth = month,
            activeDates = activeDates,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CalendarUiState(),
    )

    fun previousMonth() {
        displayedMonth.update { it.minusMonths(1) }
    }

    fun nextMonth() {
        displayedMonth.update { it.plusMonths(1) }
    }

    fun selectMonth(month: YearMonth) {
        displayedMonth.value = month
    }

    fun selectDate(date: LocalDate, onDateSelected: (LocalDate) -> Unit) {
        onDateSelected(date)
    }
}

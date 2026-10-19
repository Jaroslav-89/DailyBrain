package com.jaroapps.dailybrain.feature.history.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jaroapps.dailybrain.common.domain.model.TrainingSession
import com.jaroapps.dailybrain.common.domain.usecase.ObserveCompletedTrainingsByDateUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class DayHistoryViewModel(
    savedStateHandle: SavedStateHandle,
    observeCompletedTrainingsByDate: ObserveCompletedTrainingsByDateUseCase,
) : ViewModel() {

    val date: LocalDate = savedStateHandle.get<Int>("date")?.let { dateKey ->
        LocalDate.parse(dateKey.toString(), DateTimeFormatter.BASIC_ISO_DATE)
    } ?: LocalDate.now()

    private val _sessions: StateFlow<List<TrainingSession>> =
        observeCompletedTrainingsByDate(date.toIntKey()).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    val sessions: StateFlow<List<TrainingSession>> = _sessions
}

private fun LocalDate.toIntKey(): Int =
    format(DateTimeFormatter.BASIC_ISO_DATE).toInt()

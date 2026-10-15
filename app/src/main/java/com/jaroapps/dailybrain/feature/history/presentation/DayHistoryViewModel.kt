package com.jaroapps.dailybrain.feature.history.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jaroapps.dailybrain.common.domain.model.TrainingSession
import com.jaroapps.dailybrain.feature.history.domain.repository.HistoryRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class DayHistoryViewModel(
    savedStateHandle: SavedStateHandle,
    repository: HistoryRepository,
) : ViewModel() {

    val date: LocalDate = savedStateHandle
        .get<Int>("date")
        ?.let { dateKey ->
            LocalDate.parse(
                dateKey.toString(),
                DateTimeFormatter.BASIC_ISO_DATE,
            )
        }
        ?: LocalDate.now()

    val sessions: StateFlow<List<TrainingSession>> =
        repository.observeByDate(date.toIntKey())
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList(),
            )
}

private fun LocalDate.toIntKey(): Int =
    format(DateTimeFormatter.BASIC_ISO_DATE).toInt()

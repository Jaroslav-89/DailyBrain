package com.jaroapps.dailybrain.feature.history.presentation

import java.time.LocalDate
import java.time.YearMonth

data class CalendarUiState(
    val displayedMonth: YearMonth = YearMonth.now(),
    val activeDates: Set<LocalDate> = emptySet(),
    val firstTrainingDate: LocalDate? = null,
)

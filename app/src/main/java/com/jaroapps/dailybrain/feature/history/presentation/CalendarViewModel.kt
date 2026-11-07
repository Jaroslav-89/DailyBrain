package com.jaroapps.dailybrain.feature.history.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jaroapps.dailybrain.common.domain.model.ExerciseType
import com.jaroapps.dailybrain.common.domain.model.TrainingStreakCalculator
import com.jaroapps.dailybrain.common.domain.usecase.ObserveActiveTrainingDatesUseCase
import com.jaroapps.dailybrain.common.domain.usecase.ObserveAllCompletedTrainingsUseCase
import com.jaroapps.dailybrain.common.domain.usecase.ObservePatchedDatesUseCase
import com.jaroapps.dailybrain.feature.rewards.domain.model.WeeklyAchievementRules
import com.jaroapps.dailybrain.feature.rewards.domain.model.MonthlyChallengeRules
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId

class CalendarViewModel(
    observeActiveTrainingDates: ObserveActiveTrainingDatesUseCase,
    observePatchedDates: ObservePatchedDatesUseCase,
    observeAllCompletedTrainings: ObserveAllCompletedTrainingsUseCase,
) : ViewModel() {

    private val displayedMonth = MutableStateFlow(YearMonth.now())
    private val clock = flow {
        while (true) {
            emit(LocalDateTime.now())
            delay(60_000L)
        }
    }

    private val _uiState: StateFlow<CalendarUiState> = combine(
        displayedMonth,
        observeActiveTrainingDates(),
        observePatchedDates(),
        observeAllCompletedTrainings(),
        clock,
    ) { month, dateKeys, patchDateKeys, sessions, currentTime ->
        val trainingDates = TrainingStreakCalculator.parseDateKeys(dateKeys)
        val patchedDates = TrainingStreakCalculator.parseDateKeys(patchDateKeys)
        val today = currentTime.toLocalDate()
        val weekStart = today.with(DayOfWeek.MONDAY)
        val zone = ZoneId.systemDefault()
        val sessionsWithDates = sessions.mapNotNull { session ->
            val date = runCatching {
                Instant.ofEpochMilli(session.finishedAtEpochMs).atZone(zone).toLocalDate()
            }.getOrNull() ?: return@mapNotNull null
            date to session.exerciseType
        }
        val exerciseTypesThisWeek = sessionsWithDates
            .filter { (date, _) -> !date.isBefore(weekStart) && !date.isAfter(today) }
            .map { (_, exerciseType) -> exerciseType }
        val weeklyCounts = WeeklyAchievementRules.weeklyChallengeProgress(exerciseTypesThisWeek)
        val weeklyConditions = WeeklyAchievementRules.DEFAULT_WEEKLY_CHALLENGE_REQUIREMENTS.map { requirement ->
            requirement.toUiCondition(weeklyCounts[requirement.exerciseType] ?: 0)
        }

        // Monthly targets stay independent from weekly challenge generation. They count
        // completed sessions in the current calendar month toward a four-week volume target.
        val currentMonth = YearMonth.from(today)
        val exerciseTypesThisMonth = sessionsWithDates
            .filter { (date, _) -> YearMonth.from(date) == currentMonth }
            .map { (_, exerciseType) -> exerciseType }
        val monthlyCounts = MonthlyChallengeRules.progress(exerciseTypesThisMonth)
        val monthlyConditions = MonthlyChallengeRules.DEFAULT_MONTHLY_CHALLENGE_REQUIREMENTS.map { requirement ->
            requirement.toUiCondition(monthlyCounts[requirement.exerciseType] ?: 0)
        }

        CalendarUiState(
            displayedMonth = month,
            activeDates = trainingDates,
            patchedDates = patchedDates,
            firstTrainingDate = trainingDates.minOrNull(),
            weeklyChallengeConditions = weeklyConditions,
            monthlyChallengeConditions = monthlyConditions,
            currentTime = currentTime,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CalendarUiState(),
    )

    val uiState: StateFlow<CalendarUiState> = _uiState

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

private fun MonthlyChallengeRules.ExerciseRequirement.toUiCondition(
    completedCount: Int,
): WeeklyChallengeConditionUiState {
    val type = ExerciseType.fromStorage(exerciseType)
    return WeeklyChallengeConditionUiState(
        exerciseType = exerciseType,
        title = when (type) {
            ExerciseType.ARITHMETIC -> "Арифметика"
            ExerciseType.MEMORY_WORDS -> "Запоминание слов"
            ExerciseType.STROOP_TEST -> "Тест Струпа"
            ExerciseType.COUNTING_TRAINING -> "Устный счёт"
            null -> exerciseType.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }
        },
        completedCount = completedCount,
        requiredCount = requiredCount,
    )
}

private fun WeeklyAchievementRules.ExerciseRequirement.toUiCondition(
    completedCount: Int,
): WeeklyChallengeConditionUiState {
    val type = ExerciseType.fromStorage(exerciseType)
    return WeeklyChallengeConditionUiState(
        exerciseType = exerciseType,
        title = when (type) {
            ExerciseType.ARITHMETIC -> "Арифметика"
            ExerciseType.MEMORY_WORDS -> "Запоминание слов"
            ExerciseType.STROOP_TEST -> "Тест Струпа"
            ExerciseType.COUNTING_TRAINING -> "Устный счёт"
            null -> exerciseType.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }
        },
        completedCount = completedCount,
        requiredCount = requiredCount,
    )
}

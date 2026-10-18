package com.jaroapps.dailybrain.feature.statistics.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jaroapps.dailybrain.common.domain.model.ExerciseType
import com.jaroapps.dailybrain.common.domain.model.TrainingSession
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

            fun isInLastSevenDays(session: TrainingSession): Boolean {
                val date = Instant.ofEpochMilli(session.finishedAtEpochMs)
                    .atZone(zone)
                    .toLocalDate()

                return !date.isBefore(weekStart) && !date.isAfter(today)
            }

            fun calculateStats(
                filteredSessions: List<TrainingSession>,
            ): ExerciseStatistics {
                val weeklySessions =
                    filteredSessions.filter(::isInLastSevenDays)

                val totalTasks = filteredSessions.sumOf {
                    it.taskCount.coerceAtLeast(0)
                }
                val totalCorrect = filteredSessions.sumOf {
                    it.correctCount.coerceAtLeast(0)
                }

                return ExerciseStatistics(
                    trainingCount = filteredSessions.size,
                    weeklyTrainingCount = weeklySessions.size,
                    totalDurationMs = filteredSessions.sumOf {
                        it.durationMs.coerceAtLeast(0L)
                    },
                    averageDurationMs = filteredSessions
                        .takeIf { it.isNotEmpty() }
                        ?.map { it.durationMs.coerceAtLeast(0L) }
                        ?.average()
                        ?.toLong()
                        ?: 0L,
                    averageAccuracyPercent = if (totalTasks > 0) {
                        totalCorrect.toDouble() / totalTasks * 100.0
                    } else {
                        0.0
                    },
                    bestAccuracyPercent = filteredSessions
                        .maxOfOrNull {
                            it.accuracyPercent.coerceIn(0.0, 100.0)
                        } ?: 0.0,
                    totalTaskCount = totalTasks,
                    totalCorrectCount = totalCorrect,
                )
            }

            StatisticsUiState(
                overall = calculateStats(sessions),
                arithmetic = calculateStats(
                    sessions.filter { it.exerciseType == ExerciseType.ARITHMETIC.storageValue }
                ),
                memoryWords = calculateStats(
                    sessions.filter { it.exerciseType == ExerciseType.MEMORY_WORDS.storageValue}
                ),
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = StatisticsUiState(),
        )
}

package com.jaroapps.dailybrain.feature.statistics.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jaroapps.dailybrain.common.domain.model.ExerciseType
import com.jaroapps.dailybrain.common.domain.model.TrainingSession
import com.jaroapps.dailybrain.common.domain.usecase.ObserveAllCompletedTrainingsUseCase
import com.jaroapps.dailybrain.core.database.AppDatabase
import com.jaroapps.dailybrain.core.database.entity.ExerciseStatisticsEntity
import com.jaroapps.dailybrain.core.database.entity.OverallStatisticsEntity
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class StatisticsViewModel(
    observeAllCompletedTrainings: ObserveAllCompletedTrainingsUseCase,
    database: AppDatabase,
) : ViewModel() {

    private val aggregateState = combine(
        database.statisticsDao().observeOverall(),
        database.statisticsDao().observeExerciseStatistics(),
        database.dailyExerciseResultDao().observeAll(),
    ) { overall, exerciseStats, dailyResults ->
        AggregateStatistics(overall = overall, exerciseStats = exerciseStats, dailyResults = dailyResults)
    }

    private val _uiState: StateFlow<StatisticsUiState> = combine(
        observeAllCompletedTrainings(),
        aggregateState,
    ) { sessions, aggregate ->
            val zone = ZoneId.systemDefault()
            val today = LocalDate.now(zone)
            val weekStart = today.minusDays(6)

            fun isInLastSevenDays(session: TrainingSession): Boolean {
                val date = Instant.ofEpochMilli(session.finishedAtEpochMs).atZone(zone).toLocalDate()
                return !date.isBefore(weekStart) && !date.isAfter(today)
            }

            fun calculateStats(filteredSessions: List<TrainingSession>): ExerciseStatistics {
                val weeklySessions = filteredSessions.filter(::isInLastSevenDays)
                val totalTasks = filteredSessions.sumOf { it.taskCount.coerceAtLeast(0) }
                val totalCorrect = filteredSessions.sumOf { it.correctCount.coerceAtLeast(0) }

                return ExerciseStatistics(
                    trainingCount = filteredSessions.size,
                    weeklyTrainingCount = weeklySessions.size,
                    totalDurationMs = filteredSessions.sumOf { it.durationMs.coerceAtLeast(0L) },
                    averageDurationMs = filteredSessions.takeIf { it.isNotEmpty() }
                        ?.map { it.durationMs.coerceAtLeast(0L) }?.average()?.toLong() ?: 0L,
                    averageAccuracyPercent = if (totalTasks > 0) {
                        totalCorrect.toDouble() / totalTasks * 100.0
                    } else 0.0,
                    bestAccuracyPercent = filteredSessions
                        .filter {
                            it.taskCount > 0 &&
                                (it.exerciseType == ExerciseType.ARITHMETIC.storageValue ||
                                    it.exerciseType == ExerciseType.MEMORY_WORDS.storageValue)
                        }
                        .maxOfOrNull { it.accuracyPercent.coerceIn(0.0, 100.0) } ?: 0.0,
                    totalTaskCount = totalTasks,
                    totalCorrectCount = totalCorrect,
                )
            }

            val arithmeticAggregate = aggregate.exerciseStats.firstOrNull {
                it.exerciseType == ExerciseType.ARITHMETIC.storageValue
            }
            val memoryAggregate = aggregate.exerciseStats.firstOrNull {
                it.exerciseType == ExerciseType.MEMORY_WORDS.storageValue
            }
            val countingAggregate = aggregate.exerciseStats.firstOrNull {
                it.exerciseType == ExerciseType.COUNTING_TRAINING.storageValue
            }
            val stroopAggregate = aggregate.exerciseStats.firstOrNull {
                it.exerciseType == ExerciseType.STROOP_TEST.storageValue
            }

            StatisticsUiState(
                overall = calculateStats(sessions),
                arithmetic = calculateStats(sessions.filter {
                    it.exerciseType == ExerciseType.ARITHMETIC.storageValue
                }),
                memoryWords = calculateStats(sessions.filter {
                    it.exerciseType == ExerciseType.MEMORY_WORDS.storageValue
                }),
                countingTraining = calculateStats(sessions.filter {
                    it.exerciseType == ExerciseType.COUNTING_TRAINING.storageValue
                }),
                stroopTest = calculateStats(sessions.filter {
                    it.exerciseType == ExerciseType.STROOP_TEST.storageValue
                }),
                currentStreakDays = aggregate.overall?.currentStreakDays ?: 0,
                bestStreakDays = aggregate.overall?.bestStreakDays ?: 0,
                arithmeticBestDurationMs = arithmeticAggregate?.bestDurationMs,
                memoryBestCorrectCount = memoryAggregate?.bestCorrectCount,
                memoryBestDurationMs = memoryAggregate?.bestDurationMs,
                countingBestDurationMs = countingAggregate?.bestDurationMs,
                stroopBestDurationMs = stroopAggregate?.bestDurationMs,
                dailyResults = aggregate.dailyResults,
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = StatisticsUiState(),
        )


    val uiState: StateFlow<StatisticsUiState> = _uiState

    private data class AggregateStatistics(
        val overall: OverallStatisticsEntity?,
        val exerciseStats: List<ExerciseStatisticsEntity>,
        val dailyResults: List<com.jaroapps.dailybrain.core.database.entity.DailyExerciseResultEntity>,
    )
}

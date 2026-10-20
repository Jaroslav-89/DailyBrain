package com.jaroapps.dailybrain.feature.profile.domain.usecase

import com.jaroapps.dailybrain.common.domain.model.ExerciseType
import com.jaroapps.dailybrain.common.domain.usecase.ObserveAllCompletedTrainingsUseCase
import com.jaroapps.dailybrain.core.database.AppDatabase
import com.jaroapps.dailybrain.core.database.entity.ExerciseStatisticsEntity
import com.jaroapps.dailybrain.core.database.entity.OverallStatisticsEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

data class ProfileStatistics(
    val totalExerciseCount: Int = 0,
    val lastSevenDaysExerciseCount: Int = 0,
    val totalDurationMs: Long = 0L,
    val currentStreakDays: Int = 0,
    val bestStreakDays: Int = 0,
    val firstTrainingDate: LocalDate? = null,
    val bestExerciseStats: List<ExerciseStatisticsEntity> = emptyList(),
)

class ObserveProfileStatisticsUseCase(
    private val observeAllCompletedTrainings: ObserveAllCompletedTrainingsUseCase,
    private val database: AppDatabase,
) {
    operator fun invoke(): Flow<ProfileStatistics> = combine(
        observeAllCompletedTrainings(),
        database.statisticsDao().observeOverall(),
        database.statisticsDao().observeExerciseStatistics(),
    ) { sessions, overall, exerciseStats ->
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val weekStart = today.minusDays(6)
        val trainingDates = sessions.map { session ->
            Instant.ofEpochMilli(session.finishedAtEpochMs).atZone(zone).toLocalDate()
        }.toSet()
        var currentStreak = 0
        var cursor = today.takeIf { it in trainingDates } ?: today.minusDays(1).takeIf { it in trainingDates }
        while (cursor != null && cursor in trainingDates) {
            currentStreak++
            cursor = cursor.minusDays(1)
        }
        val recentSessions = sessions.filter { session ->
            val date = Instant.ofEpochMilli(session.finishedAtEpochMs).atZone(zone).toLocalDate()
            !date.isBefore(weekStart) && !date.isAfter(today)
        }
        ProfileStatistics(
            totalExerciseCount = sessions.size,
            lastSevenDaysExerciseCount = recentSessions.size,
            totalDurationMs = overall?.totalDurationMs ?: sessions.sumOf { it.durationMs.coerceAtLeast(0L) },
            currentStreakDays = currentStreak,
            bestStreakDays = overall?.bestStreakDays ?: 0,
            firstTrainingDate = sessions.minOfOrNull {
                Instant.ofEpochMilli(it.finishedAtEpochMs).atZone(zone).toLocalDate()
            },
            bestExerciseStats = exerciseStats,
        )
    }
}

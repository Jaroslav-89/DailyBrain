package com.jaroapps.dailybrain.feature.profile.data.repository

import com.jaroapps.dailybrain.common.domain.model.TrainingSession
import com.jaroapps.dailybrain.core.database.AppDatabase
import com.jaroapps.dailybrain.feature.profile.domain.repository.ProfileStatistics
import com.jaroapps.dailybrain.feature.profile.domain.repository.BestExerciseResult
import com.jaroapps.dailybrain.feature.profile.domain.repository.ProfileStatistics
import com.jaroapps.dailybrain.feature.profile.domain.repository.ProfileStatisticsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class ProfileStatisticsRepositoryImpl(
    private val database: AppDatabase,
) : ProfileStatisticsRepository {
    override fun observeProfileStatistics(): Flow<ProfileStatistics> = combine(
        database.trainingSessionDao().observeAll().map { entities ->
            entities.map { entity ->
                TrainingSession(
                    id = entity.id,
                    exerciseType = entity.exerciseType,
                    startedAtEpochMs = entity.startedAtEpochMs,
                    finishedAtEpochMs = entity.finishedAtEpochMs,
                    durationMs = entity.durationMs,
                    accuracyPercent = entity.accuracyPercent,
                    taskCount = entity.taskCount,
                    correctCount = entity.correctCount,
                )
            }
        },
        database.statisticsDao().observeOverall(),
        database.statisticsDao().observeExerciseStatistics(),
    ) { sessions, overall, exerciseStats ->
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val trainingDates = sessions.map { Instant.ofEpochMilli(it.finishedAtEpochMs).atZone(zone).toLocalDate() }.toSet()
        val weekStart = today.minusDays(6)
        val recentSessions = sessions.count { session ->
            val date = Instant.ofEpochMilli(session.finishedAtEpochMs).atZone(zone).toLocalDate()
            !date.isBefore(weekStart) && !date.isAfter(today)
        }
        var streak = 0
        var cursor: LocalDate? = today.takeIf { it in trainingDates }
            ?: today.minusDays(1).takeIf { it in trainingDates }
        while (cursor != null && cursor in trainingDates) {
            streak++
            cursor = cursor.minusDays(1)
        }
        ProfileStatistics(
            totalExerciseCount = sessions.size,
            lastSevenDaysExerciseCount = recentSessions,
            totalDurationMs = overall?.totalDurationMs ?: sessions.sumOf { it.durationMs.coerceAtLeast(0L) },
            currentStreakDays = streak,
            bestStreakDays = overall?.bestStreakDays ?: 0,
            firstTrainingDate = trainingDates.minOrNull(),
            bestExerciseStats = exerciseStats.map { BestExerciseResult(it.exerciseType, it.bestDurationMs, it.bestCorrectCount) },
        )
    }
}

package com.jaroapps.dailybrain.feature.statistics.data.repository

import com.jaroapps.dailybrain.core.database.ActiveUserProvider
import com.jaroapps.dailybrain.core.database.AppDatabase
import com.jaroapps.dailybrain.feature.statistics.domain.model.DailyExerciseResult
import com.jaroapps.dailybrain.feature.statistics.domain.model.ExerciseStatistics
import com.jaroapps.dailybrain.feature.statistics.domain.model.OverallStatistics
import com.jaroapps.dailybrain.feature.statistics.domain.model.StatisticsAggregate
import com.jaroapps.dailybrain.feature.statistics.domain.repository.StatisticsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class StatisticsRepositoryImpl(
    private val database: AppDatabase,
    private val activeUserProvider: ActiveUserProvider,
) : StatisticsRepository {
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    override fun observeStatistics(): Flow<StatisticsAggregate> =
        activeUserProvider.observeUserId().flatMapLatest { userId ->
            combine(
                database.statisticsDao().observeOverall(userId),
                database.statisticsDao().observeExerciseStatistics(userId),
                database.dailyExerciseResultDao().observeAll(userId),
            ) { overall, exerciseStatistics, dailyResults ->
                StatisticsAggregate(
                    overall = overall?.let { OverallStatistics(it.currentStreakDays, it.bestStreakDays) },
                    exerciseStatistics = exerciseStatistics.map {
                        ExerciseStatistics(it.exerciseType, it.bestDurationMs, it.bestAccuracyPercent, it.bestCorrectCount)
                    },
                    dailyResults = dailyResults.map {
                        DailyExerciseResult(it.date, it.arithmeticBestTimeMs, it.memoryBestCorrectCount, it.stroopBestTimeMs, it.countingBestTimeMs)
                    },
                )
            }
        }
}
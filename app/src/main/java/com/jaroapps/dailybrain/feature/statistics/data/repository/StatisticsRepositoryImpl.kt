package com.jaroapps.dailybrain.feature.statistics.data.repository

import com.jaroapps.dailybrain.core.database.AppDatabase
import com.jaroapps.dailybrain.feature.statistics.domain.model.DailyExerciseResult
import com.jaroapps.dailybrain.feature.statistics.domain.model.ExerciseStatistics
import com.jaroapps.dailybrain.feature.statistics.domain.model.OverallStatistics
import com.jaroapps.dailybrain.feature.statistics.domain.model.StatisticsAggregate
import com.jaroapps.dailybrain.feature.statistics.domain.repository.StatisticsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class StatisticsRepositoryImpl(
    private val database: AppDatabase,
) : StatisticsRepository {

    override fun observeStatistics(): Flow<StatisticsAggregate> = combine(
        database.statisticsDao().observeOverall(),
        database.statisticsDao().observeExerciseStatistics(),
        database.dailyExerciseResultDao().observeAll(),
    ) { overall, exerciseStatistics, dailyResults ->
        StatisticsAggregate(
            overall = overall?.let {
                OverallStatistics(
                    currentStreakDays = it.currentStreakDays,
                    bestStreakDays = it.bestStreakDays,
                )
            },
            exerciseStatistics = exerciseStatistics.map {
                ExerciseStatistics(
                    exerciseType = it.exerciseType,
                    bestDurationMs = it.bestDurationMs,
                    bestAccuracyPercent = it.bestAccuracyPercent,
                    bestCorrectCount = it.bestCorrectCount,
                )
            },
            dailyResults = dailyResults.map {
                DailyExerciseResult(
                    date = it.date,
                    arithmeticBestTimeMs = it.arithmeticBestTimeMs,
                    memoryBestCorrectCount = it.memoryBestCorrectCount,
                    stroopBestTimeMs = it.stroopBestTimeMs,
                    countingBestTimeMs = it.countingBestTimeMs,
                )
            },
        )
    }
}

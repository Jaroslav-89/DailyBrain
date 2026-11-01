package com.jaroapps.dailybrain.feature.statistics.data.repository

import com.jaroapps.dailybrain.common.domain.model.TrainingStreakCalculator
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
import java.time.LocalDate

class StatisticsRepositoryImpl(
    private val database: AppDatabase,
    private val activeUserProvider: ActiveUserProvider,
) : StatisticsRepository {
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    override fun observeStatistics(): Flow<StatisticsAggregate> =
        activeUserProvider.observeUserId().flatMapLatest { userId ->
            combine(
                database.trainingDayDao().observeAll(userId),
                database.statisticsDao().observeExerciseStatistics(userId),
                database.dailyExerciseResultDao().observeAll(userId),
            ) { trainingDays, exerciseStatistics, dailyResults ->
                val activeDates = TrainingStreakCalculator.parseDateKeys(trainingDays.map { it.date })
                val streak = TrainingStreakCalculator.calculate(activeDates, LocalDate.now())
                StatisticsAggregate(
                    overall = OverallStatistics(streak.currentDays, streak.bestDays),
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
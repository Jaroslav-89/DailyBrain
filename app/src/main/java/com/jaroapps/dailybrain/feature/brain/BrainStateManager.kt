package com.jaroapps.dailybrain.feature.brain

import com.jaroapps.dailybrain.common.domain.model.TrainingStreakCalculator
import android.content.Context
import com.jaroapps.dailybrain.core.database.ActiveUserProvider
import com.jaroapps.dailybrain.core.database.AppDatabase
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Resolves the two-state mood from today's saved training and the local time.
 * The database is the source of truth, so a new day naturally resets the mood.
 */
class BrainStateManager(
    private val context: Context,
    private val database: AppDatabase,
    private val activeUserProvider: ActiveUserProvider,
) {
    suspend fun resolveMood(): BrainMood {
        val userId = activeUserProvider.currentUserId()
        val today = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE).toInt()
        val hasTrainingToday = database.trainingDayDao().hasTrainingOnDate(userId, today)

        return when {
            hasTrainingToday -> BrainMood.BASE
            LocalTime.now().hour >= 20 -> BrainMood.SOUR
            else -> BrainMood.BASE
        }
    }

    suspend fun refresh() {
        val userId = activeUserProvider.currentUserId()
        val today = LocalDate.now()
        val days = database.trainingDayDao().getAll(userId)
        val trainedDates = TrainingStreakCalculator.parseDateKeys(days.map { it.date })
        val hasTrainingToday = today in trainedDates
        val mood = when {
            hasTrainingToday -> BrainMood.BASE
            LocalTime.now().hour >= 20 -> BrainMood.SOUR
            else -> BrainMood.BASE
        }
        val streak = TrainingStreakCalculator.calculate(trainedDates, today)
        BrainWidgetUpdater.update(context, mood, streak.currentDays, hasTrainingToday)
    }
}

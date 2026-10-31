package com.jaroapps.dailybrain.feature.brain

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
        val trainedDates = days.mapNotNull { day ->
            runCatching {
                LocalDate.parse(day.date.toString(), DateTimeFormatter.BASIC_ISO_DATE)
            }.getOrNull()
        }.toSet()
        val hasTrainingToday = today in trainedDates
        val mood = when {
            hasTrainingToday -> BrainMood.BASE
            LocalTime.now().hour >= 20 -> BrainMood.SOUR
            else -> BrainMood.BASE
        }
        var streak = 0
        var date = if (hasTrainingToday) today else today.minusDays(1)
        while (date in trainedDates) {
            streak++
            date = date.minusDays(1)
        }

        BrainWidgetUpdater.update(context, mood, streak, hasTrainingToday)
    }
}

package com.jaroapps.dailybrain.feature.rewards.domain.model

import java.time.LocalDate

/**
 * Completion rules shared by the weekly calendar streak and weekly challenge.
 *
 * The streak is based on active calendar dates, regardless of exercise type. Its
 * caller supplies dates from the training_days table, which is the same day index
 * used by the calendar UI.
 *
 * The weekly challenge is stricter: arithmetic must be completed on each of the
 * seven dates in the ISO week, and all three additional exercise types must also
 * be completed during that same week.
 */
object WeeklyAchievementRules {

    const val REQUIRED_WEEK_DAYS = 7

    fun weeklyStreakCompletionDates(
        trainingDates: Collection<LocalDate>,
    ): List<LocalDate> {
        val dates = trainingDates.distinct().sorted()
        if (dates.isEmpty()) return emptyList()

        val completions = mutableListOf<LocalDate>()
        var streakLength = 0
        var previousDate: LocalDate? = null

        dates.forEach { date ->
            streakLength = if (previousDate?.plusDays(1) == date) streakLength + 1 else 1
            previousDate = date
            if (streakLength % REQUIRED_WEEK_DAYS == 0) {
                completions += date
            }
        }

        return completions
    }

    fun isWeeklyChallengeCompleted(
        weekStart: LocalDate,
        arithmeticTrainingDates: Set<LocalDate>,
        completedExerciseTypes: Set<String>,
        requiredAdditionalExerciseTypes: Set<String>,
    ): Boolean {
        if (requiredAdditionalExerciseTypes.isEmpty()) return false

        val hasArithmeticEveryDay = (0 until REQUIRED_WEEK_DAYS).all { offset ->
            weekStart.plusDays(offset.toLong()) in arithmeticTrainingDates
        }
        val hasAllAdditionalExercises =
            completedExerciseTypes.containsAll(requiredAdditionalExerciseTypes)

        return hasArithmeticEveryDay && hasAllAdditionalExercises
    }
}

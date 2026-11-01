package com.jaroapps.dailybrain.feature.rewards.domain.model

import java.time.LocalDate

/**
 * Pure eligibility rules for the two weekly achievements.
 *
 * A weekly streak is a rolling sequence of seven consecutive dates with at least one
 * arithmetic session per date. The weekly challenge is scoped to an ISO Monday-Sunday
 * week: arithmetic on every date plus at least one of the configured additional exercises.
 */
object WeeklyAchievementRules {

    fun weeklyStreakCompletionDates(
        arithmeticTrainingDates: Collection<LocalDate>,
    ): List<LocalDate> {
        val dates = arithmeticTrainingDates.distinct().sorted()
        if (dates.isEmpty()) return emptyList()

        val completions = mutableListOf<LocalDate>()
        var streakLength = 0
        var previousDate: LocalDate? = null

        dates.forEach { date ->
            streakLength = if (previousDate?.plusDays(1) == date) streakLength + 1 else 1
            previousDate = date
            if (streakLength % 7 == 0) completions += date
        }

        return completions
    }

    fun isWeeklyChallengeCompleted(
        weekStart: LocalDate,
        arithmeticDates: Set<LocalDate>,
        completedExerciseTypes: Set<String>,
        additionalExerciseTypes: Set<String>,
    ): Boolean {
        val hasArithmeticEveryDay = (0..6).all { offset ->
            weekStart.plusDays(offset.toLong()) in arithmeticDates
        }
        val hasAtLeastOneAdditionalExercise =
            additionalExerciseTypes.any { it in completedExerciseTypes }

        return hasArithmeticEveryDay && hasAtLeastOneAdditionalExercise
    }
}

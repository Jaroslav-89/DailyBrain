package com.jaroapps.dailybrain.feature.rewards.domain.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/**
 * Completion rules shared by the weekly calendar streak and weekly challenge.
 *
 * The streak is a rolling sequence of active calendar dates, regardless of
 * ISO-week boundaries or exercise type. The first active date is day one, and a
 * reward is completed on every seventh consecutive active date.
 *
 * The weekly challenge is calendar-based: arithmetic must be completed on each
 * of the seven dates from Monday through Sunday, and all three additional
 * exercise types must also be completed during that same ISO week.
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

    /**
     * Returns every ISO-week start (Monday) whose seven days contain arithmetic and
     * every required additional exercise type. Dates from adjacent weeks are never mixed.
     */
    fun weeklyChallengeCompletionWeekStarts(
        exerciseTypesByDate: Map<LocalDate, Set<String>>,
        arithmeticExerciseType: String,
        requiredAdditionalExerciseTypes: Set<String>,
    ): List<LocalDate> {
        if (exerciseTypesByDate.isEmpty() || requiredAdditionalExerciseTypes.isEmpty()) {
            return emptyList()
        }

        val candidateWeekStarts = exerciseTypesByDate.keys
            .map { date -> date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)) }
            .distinct()
            .sorted()

        return candidateWeekStarts.filter { weekStart ->
            val weekDates = (0 until REQUIRED_WEEK_DAYS)
                .map { offset -> weekStart.plusDays(offset.toLong()) }
            val arithmeticDates = weekDates.filter { date ->
                arithmeticExerciseType in exerciseTypesByDate[date].orEmpty()
            }.toSet()
            val completedAdditionalTypes = weekDates
                .flatMap { date -> exerciseTypesByDate[date].orEmpty() }
                .toSet()

            isWeeklyChallengeCompleted(
                weekStart = weekStart,
                arithmeticTrainingDates = arithmeticDates,
                completedExerciseTypes = completedAdditionalTypes,
                requiredAdditionalExerciseTypes = requiredAdditionalExerciseTypes,
            )
        }
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

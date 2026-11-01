package com.jaroapps.dailybrain.feature.rewards.domain.model

import java.time.LocalDate

/**
 * Shared completion rules for weekly streak and weekly challenge.
 *
 * A weekly streak is seven consecutive calendar dates with at least one arithmetic
 * session on each date. The weekly challenge is intentionally different: it requires
 * at least seven arithmetic sessions anywhere in the ISO week, plus all three
 * additional exercise types at least once.
 *
 * RewardRecalculator uses these same predicates before creating the XP and endorphin
 * ledger entries, so the two currencies cannot disagree about achievement eligibility.
 */
object WeeklyAchievementRules {

    const val REQUIRED_WEEKLY_ARITHMETIC_SESSIONS = 7

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
            if (streakLength % REQUIRED_WEEKLY_ARITHMETIC_SESSIONS == 0) {
                completions += date
            }
        }

        return completions
    }

    fun isWeeklyChallengeCompleted(
        arithmeticSessionCount: Int,
        completedExerciseTypes: Set<String>,
        requiredAdditionalExerciseTypes: Set<String>,
    ): Boolean {
        if (arithmeticSessionCount < REQUIRED_WEEKLY_ARITHMETIC_SESSIONS) return false
        if (requiredAdditionalExerciseTypes.isEmpty()) return false
        return completedExerciseTypes.containsAll(requiredAdditionalExerciseTypes)
    }
}

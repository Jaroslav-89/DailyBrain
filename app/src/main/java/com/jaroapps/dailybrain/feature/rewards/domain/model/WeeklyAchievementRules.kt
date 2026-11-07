package com.jaroapps.dailybrain.feature.rewards.domain.model

import com.jaroapps.dailybrain.common.domain.model.ExerciseType
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/**
 * Rules for recurring achievements and the data-driven weekly exercise challenge.
 *
 * Streaks count unique active calendar dates (real training or a protected date).
 * Weekly challenges count completed sessions, not distinct dates.
 */
object WeeklyAchievementRules {

    const val REQUIRED_WEEK_DAYS = 7

    data class ExerciseRequirement(
        val exerciseType: String,
        val requiredCount: Int,
    ) {
        init {
            require(exerciseType.isNotBlank())
            require(requiredCount > 0)
        }
    }

    /**
     * The current default challenge. Future generated challenges can provide another list of
     * requirements without changing progress or completion calculation.
     */
    val DEFAULT_WEEKLY_CHALLENGE_REQUIREMENTS = listOf(
        ExerciseRequirement(ExerciseType.ARITHMETIC.storageValue, 7),
        ExerciseRequirement(ExerciseType.MEMORY_WORDS.storageValue, 1),
        ExerciseRequirement(ExerciseType.STROOP_TEST.storageValue, 1),
        ExerciseRequirement(ExerciseType.COUNTING_TRAINING.storageValue, 1),
    )

    fun weeklyStreakCompletionDates(
        activeDates: Collection<LocalDate>,
    ): List<LocalDate> {
        val dates = activeDates.distinct().sorted()
        if (dates.isEmpty()) return emptyList()

        val completions = mutableListOf<LocalDate>()
        var streakLength = 0
        var previousDate: LocalDate? = null
        dates.forEach { date ->
            streakLength = if (previousDate?.plusDays(1) == date) streakLength + 1 else 1
            previousDate = date
            if (streakLength % REQUIRED_WEEK_DAYS == 0) completions += date
        }
        return completions
    }

    /** Counts sessions of each exercise type for one Monday-Sunday calendar week. */
    fun weeklyChallengeProgress(
        completedExerciseTypes: Collection<String>,
        requirements: List<ExerciseRequirement> = DEFAULT_WEEKLY_CHALLENGE_REQUIREMENTS,
    ): Map<String, Int> {
        val counts = completedExerciseTypes.groupingBy { it }.eachCount()
        return requirements.associate { requirement ->
            requirement.exerciseType to minOf(
                counts[requirement.exerciseType] ?: 0,
                requirement.requiredCount,
            )
        }
    }

    fun isWeeklyChallengeCompleted(
        completedExerciseTypes: Collection<String>,
        requirements: List<ExerciseRequirement> = DEFAULT_WEEKLY_CHALLENGE_REQUIREMENTS,
    ): Boolean {
        if (requirements.isEmpty()) return false
        val counts = completedExerciseTypes.groupingBy { it }.eachCount()
        return requirements.all { requirement ->
            (counts[requirement.exerciseType] ?: 0) >= requirement.requiredCount
        }
    }

    /**
     * Returns week starts where all required session counts were completed.
     * Map keys are Monday dates; each collection contains one item per saved workout session,
     * so several arithmetic sessions on the same day each count.
     */
    fun weeklyChallengeCompletionWeekStarts(
        exerciseTypesByWeek: Map<LocalDate, Collection<String>>,
        requirements: List<ExerciseRequirement> = DEFAULT_WEEKLY_CHALLENGE_REQUIREMENTS,
    ): List<LocalDate> = exerciseTypesByWeek
        .filterValues { completedTypes -> isWeeklyChallengeCompleted(completedTypes, requirements) }
        .keys
        .filter { it.dayOfWeek == DayOfWeek.MONDAY }
        .sorted()

    fun weekStart(date: LocalDate): LocalDate =
        date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
}

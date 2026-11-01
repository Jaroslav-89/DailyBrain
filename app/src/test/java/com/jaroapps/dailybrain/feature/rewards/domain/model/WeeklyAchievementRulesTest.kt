package com.jaroapps.dailybrain.feature.rewards.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class WeeklyAchievementRulesTest {

    private val weekStart = LocalDate.of(2026, 10, 26) // Monday
    private val weekDates = (0..6).map { weekStart.plusDays(it.toLong()) }.toSet()
    private val additionalTypes = setOf("MEMORY_WORDS", "STROOP_TEST", "COUNTING_TRAINING")

    @Test
    fun weeklyStreakUsesAnyActiveCalendarDayRegardlessOfExerciseType() {
        // These dates are supplied by training_days, which records all exercise types.
        assertEquals(
            listOf(weekStart.plusDays(6)),
            WeeklyAchievementRules.weeklyStreakCompletionDates(weekDates),
        )
    }

    @Test
    fun duplicateWorkoutsOnOneDateCountAsOneStreakDay() {
        val calendarDates = weekDates.toList() + weekStart.plusDays(6)

        assertEquals(
            listOf(weekStart.plusDays(6)),
            WeeklyAchievementRules.weeklyStreakCompletionDates(calendarDates),
        )
    }

    @Test
    fun aMissingCalendarDateBreaksTheWeeklyStreak() {
        val activeDates = weekDates - weekStart.plusDays(3)

        assertTrue(
            WeeklyAchievementRules.weeklyStreakCompletionDates(activeDates).isEmpty(),
        )
    }

    @Test
    fun streakRewardsRepeatAfterEachFurtherSevenConsecutiveActiveDays() {
        val fourteenDays = (0..13).map { weekStart.plusDays(it.toLong()) }

        assertEquals(
            listOf(weekStart.plusDays(6), weekStart.plusDays(13)),
            WeeklyAchievementRules.weeklyStreakCompletionDates(fourteenDays),
        )
    }

    @Test
    fun weeklyChallengeRequiresArithmeticEveryDayAndAllThreeAdditionalExercises() {
        assertTrue(
            WeeklyAchievementRules.isWeeklyChallengeCompleted(
                weekStart = weekStart,
                arithmeticTrainingDates = weekDates,
                completedExerciseTypes = additionalTypes,
                requiredAdditionalExerciseTypes = additionalTypes,
            ),
        )
    }

    @Test
    fun missingArithmeticOnAnyWeekDayPreventsWeeklyChallenge() {
        assertFalse(
            WeeklyAchievementRules.isWeeklyChallengeCompleted(
                weekStart = weekStart,
                arithmeticTrainingDates = weekDates - weekStart.plusDays(4),
                completedExerciseTypes = additionalTypes,
                requiredAdditionalExerciseTypes = additionalTypes,
            ),
        )
    }

    @Test
    fun multipleArithmeticSessionsOnOneDayCannotReplaceArithmeticOnEveryDay() {
        // The calendar-date set contains one date even when that date has many sessions.
        val arithmeticDates = setOf(weekStart)

        assertFalse(
            WeeklyAchievementRules.isWeeklyChallengeCompleted(
                weekStart = weekStart,
                arithmeticTrainingDates = arithmeticDates,
                completedExerciseTypes = additionalTypes,
                requiredAdditionalExerciseTypes = additionalTypes,
            ),
        )
    }

    @Test
    fun allSevenArithmeticDaysAreNotEnoughWithoutAllThreeAdditionalExercises() {
        assertFalse(
            WeeklyAchievementRules.isWeeklyChallengeCompleted(
                weekStart = weekStart,
                arithmeticTrainingDates = weekDates,
                completedExerciseTypes = setOf("MEMORY_WORDS", "STROOP_TEST"),
                requiredAdditionalExerciseTypes = additionalTypes,
            ),
        )
    }

    @Test
    fun unknownExerciseDoesNotReplaceARequiredAdditionalExercise() {
        assertFalse(
            WeeklyAchievementRules.isWeeklyChallengeCompleted(
                weekStart = weekStart,
                arithmeticTrainingDates = weekDates,
                completedExerciseTypes = additionalTypes - "COUNTING_TRAINING" + "UNKNOWN",
                requiredAdditionalExerciseTypes = additionalTypes,
            ),
        )
    }

    @Test
    fun weeklyChallengeDoesNotCompleteWithoutAnyRequiredAdditionalExercises() {
        assertFalse(
            WeeklyAchievementRules.isWeeklyChallengeCompleted(
                weekStart = weekStart,
                arithmeticTrainingDates = weekDates,
                completedExerciseTypes = emptySet(),
                requiredAdditionalExerciseTypes = additionalTypes,
            ),
        )
    }
}

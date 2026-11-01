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
        // These local dates are derived from completed sessions of all exercise types.
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
    fun streakStartingOnWednesdayCrossesMondayAndRewardsEverySeventhActiveDate() {
        val wednesdayStart = LocalDate.of(2026, 10, 28)
        // Eight active dates: Wednesday through the following Wednesday.
        // Day seven is Tuesday; Monday does not reset the rolling streak.
        val dates = (0..7).map { wednesdayStart.plusDays(it.toLong()) }

        assertEquals(
            listOf(wednesdayStart.plusDays(6)),
            WeeklyAchievementRules.weeklyStreakCompletionDates(dates),
        )
    }

    @Test
    fun streakStartingOnWednesdayRepeatsAfterAnotherSevenActiveDates() {
        val wednesdayStart = LocalDate.of(2026, 10, 28)
        val fifteenDates = (0..14).map { wednesdayStart.plusDays(it.toLong()) }

        assertEquals(
            listOf(wednesdayStart.plusDays(6), wednesdayStart.plusDays(13)),
            WeeklyAchievementRules.weeklyStreakCompletionDates(fifteenDates),
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
    fun weeklyChallengeRewardsArePlannedForTwoConsecutiveCompletedWeeks() {
        val secondWeekStart = weekStart.plusDays(7)
        val exerciseTypesByDate = buildMap {
            (0 until 7).forEach { offset ->
                put(weekStart.plusDays(offset.toLong()), setOf("ARITHMETIC"))
                put(secondWeekStart.plusDays(offset.toLong()), setOf("ARITHMETIC"))
            }
            put(weekStart.plusDays(1), setOf("ARITHMETIC", "MEMORY_WORDS"))
            put(weekStart.plusDays(2), setOf("ARITHMETIC", "STROOP_TEST"))
            put(weekStart.plusDays(3), setOf("ARITHMETIC", "COUNTING_TRAINING"))
            put(secondWeekStart.plusDays(1), setOf("ARITHMETIC", "MEMORY_WORDS"))
            put(secondWeekStart.plusDays(2), setOf("ARITHMETIC", "STROOP_TEST"))
            put(secondWeekStart.plusDays(3), setOf("ARITHMETIC", "COUNTING_TRAINING"))
        }

        assertEquals(
            listOf(weekStart, secondWeekStart),
            WeeklyAchievementRules.weeklyChallengeCompletionWeekStarts(
                exerciseTypesByDate = exerciseTypesByDate,
                arithmeticExerciseType = "ARITHMETIC",
                requiredAdditionalExerciseTypes = additionalTypes,
            ),
        )
    }

    @Test
    fun weeklyChallengeDoesNotCombineRequirementsFromDifferentWeeks() {
        val secondWeekStart = weekStart.plusDays(7)
        val exerciseTypesByDate = buildMap {
            (0 until 7).forEach { offset ->
                put(weekStart.plusDays(offset.toLong()), setOf("ARITHMETIC"))
                put(secondWeekStart.plusDays(offset.toLong()), setOf("ARITHMETIC"))
            }
            put(weekStart.plusDays(1), setOf("ARITHMETIC", "MEMORY_WORDS"))
            put(secondWeekStart.plusDays(1), setOf("ARITHMETIC", "STROOP_TEST", "COUNTING_TRAINING"))
        }

        assertTrue(
            WeeklyAchievementRules.weeklyChallengeCompletionWeekStarts(
                exerciseTypesByDate = exerciseTypesByDate,
                arithmeticExerciseType = "ARITHMETIC",
                requiredAdditionalExerciseTypes = additionalTypes,
            ).isEmpty(),
        )
    }

    @Test
    fun missingArithmeticOnAnyWeekDayPreventsWeeklyChallenge() {
        assertFalse(
            WeeklyAchievementRules.isWeeklyChallengeCompleted(
                weekStart = weekStart,
                // The following Monday cannot replace a missing day inside this Monday-Sunday week.
                arithmeticTrainingDates = weekDates - weekStart.plusDays(4) + weekStart.plusDays(7),
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

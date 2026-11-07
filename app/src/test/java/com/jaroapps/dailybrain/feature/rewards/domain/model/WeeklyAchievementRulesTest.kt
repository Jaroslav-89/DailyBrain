package com.jaroapps.dailybrain.feature.rewards.domain.model

import com.jaroapps.dailybrain.common.domain.model.ExerciseType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class WeeklyAchievementRulesTest {

    private val weekStart = LocalDate.of(2026, 10, 26) // Monday
    private val weekDates = (0..6).map { weekStart.plusDays(it.toLong()) }.toSet()
    private val requirements = WeeklyAchievementRules.DEFAULT_WEEKLY_CHALLENGE_REQUIREMENTS

    @Test
    fun weeklyStreakUsesAnyActiveCalendarDayRegardlessOfExerciseType() {
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
        assertTrue(WeeklyAchievementRules.weeklyStreakCompletionDates(activeDates).isEmpty())
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
        val dates = (0..7).map { wednesdayStart.plusDays(it.toLong()) }
        assertEquals(
            listOf(wednesdayStart.plusDays(6)),
            WeeklyAchievementRules.weeklyStreakCompletionDates(dates),
        )
    }

    @Test
    fun streakStartingOnWednesdayRepeatsAfterAnotherSevenActiveDates() {
        val wednesdayStart = LocalDate.of(2026, 10, 28)
        val dates = (0..14).map { wednesdayStart.plusDays(it.toLong()) }
        assertEquals(
            listOf(wednesdayStart.plusDays(6), wednesdayStart.plusDays(13)),
            WeeklyAchievementRules.weeklyStreakCompletionDates(dates),
        )
    }

    @Test
    fun weeklyChallengeAcceptsSevenArithmeticSessionsOnOneDay() {
        val types = buildList {
            repeat(7) { add(ExerciseType.ARITHMETIC.storageValue) }
            add(ExerciseType.MEMORY_WORDS.storageValue)
            add(ExerciseType.STROOP_TEST.storageValue)
            add(ExerciseType.COUNTING_TRAINING.storageValue)
        }
        assertTrue(WeeklyAchievementRules.isWeeklyChallengeCompleted(types))
    }

    @Test
    fun weeklyChallengeCountsSessionsAndCapsDisplayedProgressAtRequirement() {
        val types = buildList {
            repeat(9) { add(ExerciseType.ARITHMETIC.storageValue) }
            add(ExerciseType.MEMORY_WORDS.storageValue)
        }
        assertEquals(
            mapOf(
                ExerciseType.ARITHMETIC.storageValue to 7,
                ExerciseType.MEMORY_WORDS.storageValue to 1,
                ExerciseType.STROOP_TEST.storageValue to 0,
                ExerciseType.COUNTING_TRAINING.storageValue to 0,
            ),
            WeeklyAchievementRules.weeklyChallengeProgress(types),
        )
    }

    @Test
    fun weeklyChallengeRequirementsCanBeChangedWithoutChangingCalculator() {
        val customRequirements = listOf(
            WeeklyAchievementRules.ExerciseRequirement(ExerciseType.ARITHMETIC.storageValue, 2),
            WeeklyAchievementRules.ExerciseRequirement(ExerciseType.STROOP_TEST.storageValue, 3),
        )
        val types = listOf(
            ExerciseType.ARITHMETIC.storageValue,
            ExerciseType.ARITHMETIC.storageValue,
            ExerciseType.STROOP_TEST.storageValue,
            ExerciseType.STROOP_TEST.storageValue,
            ExerciseType.STROOP_TEST.storageValue,
        )
        assertTrue(WeeklyAchievementRules.isWeeklyChallengeCompleted(types, customRequirements))
    }

    @Test
    fun requirementsFromDifferentWeeksAreNeverCombined() {
        val secondWeek = weekStart.plusDays(7)
        val firstWeekTypes = listOf(
            *List(7) { ExerciseType.ARITHMETIC.storageValue }.toTypedArray(),
            ExerciseType.MEMORY_WORDS.storageValue,
        )
        val secondWeekTypes = listOf(
            *List(7) { ExerciseType.ARITHMETIC.storageValue }.toTypedArray(),
            ExerciseType.STROOP_TEST.storageValue,
            ExerciseType.COUNTING_TRAINING.storageValue,
        )
        assertTrue(
            WeeklyAchievementRules.weeklyChallengeCompletionWeekStarts(
                mapOf(weekStart to firstWeekTypes, secondWeek to secondWeekTypes),
            ).isEmpty(),
        )
    }

    @Test
    fun challengeCanCompleteInEachOfTwoConsecutiveWeeks() {
        val completeWeek = buildList {
            repeat(7) { add(ExerciseType.ARITHMETIC.storageValue) }
            add(ExerciseType.MEMORY_WORDS.storageValue)
            add(ExerciseType.STROOP_TEST.storageValue)
            add(ExerciseType.COUNTING_TRAINING.storageValue)
        }
        assertEquals(
            listOf(weekStart, weekStart.plusDays(7)),
            WeeklyAchievementRules.weeklyChallengeCompletionWeekStarts(
                mapOf(weekStart to completeWeek, weekStart.plusDays(7) to completeWeek),
            ),
        )
    }

    @Test
    fun everyAdditionalExerciseIsStillRequired() {
        val types = buildList {
            repeat(7) { add(ExerciseType.ARITHMETIC.storageValue) }
            add(ExerciseType.MEMORY_WORDS.storageValue)
            add(ExerciseType.STROOP_TEST.storageValue)
        }
        assertFalse(WeeklyAchievementRules.isWeeklyChallengeCompleted(types))
    }

    @Test
    fun additionalArithmeticSessionsDoNotReplaceMissingAdditionalExercise() {
        val types = List(20) { ExerciseType.ARITHMETIC.storageValue } +
            ExerciseType.MEMORY_WORDS.storageValue +
            ExerciseType.STROOP_TEST.storageValue
        assertFalse(WeeklyAchievementRules.isWeeklyChallengeCompleted(types))
    }

    @Test
    fun unknownExerciseDoesNotReplaceRequiredExercise() {
        val types = List(7) { ExerciseType.ARITHMETIC.storageValue } +
            ExerciseType.MEMORY_WORDS.storageValue +
            ExerciseType.STROOP_TEST.storageValue +
            "UNKNOWN"
        assertFalse(WeeklyAchievementRules.isWeeklyChallengeCompleted(types))
    }
}

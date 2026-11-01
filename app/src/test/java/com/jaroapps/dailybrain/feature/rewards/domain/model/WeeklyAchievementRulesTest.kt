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
    fun weeklyStreakIsGrantedAfterSevenConsecutiveArithmeticDays() {
        assertEquals(
            listOf(weekStart.plusDays(6)),
            WeeklyAchievementRules.weeklyStreakCompletionDates(weekDates),
        )
    }

    @Test
    fun nonArithmeticWorkoutsDoNotExtendArithmeticWeeklyStreak() {
        val arithmeticDates = weekDates - weekStart.plusDays(3)

        assertTrue(
            WeeklyAchievementRules.weeklyStreakCompletionDates(arithmeticDates).isEmpty(),
        )
    }

    @Test
    fun duplicateArithmeticSessionsOnOneDateCountAsOneStreakDay() {
        val datesWithDuplicate = weekDates.toList() + weekStart.plusDays(6)

        assertEquals(
            listOf(weekStart.plusDays(6)),
            WeeklyAchievementRules.weeklyStreakCompletionDates(datesWithDuplicate),
        )
    }

    @Test
    fun weeklyChallengeRequiresAtLeastSevenArithmeticSessionsAndAllThreeAdditionalExercises() {
        assertFalse(
            WeeklyAchievementRules.isWeeklyChallengeCompleted(
                arithmeticSessionCount = 7,
                completedExerciseTypes = setOf("MEMORY_WORDS", "STROOP_TEST"),
                requiredAdditionalExerciseTypes = additionalTypes,
            ),
        )

        assertTrue(
            WeeklyAchievementRules.isWeeklyChallengeCompleted(
                arithmeticSessionCount = 7,
                completedExerciseTypes = additionalTypes,
                requiredAdditionalExerciseTypes = additionalTypes,
            ),
        )
    }

    @Test
    fun fewerThanSevenArithmeticSessionsDoNotCompleteWeeklyChallengeEvenWithAllAdditionalExercises() {
        assertFalse(
            WeeklyAchievementRules.isWeeklyChallengeCompleted(
                arithmeticSessionCount = 6,
                completedExerciseTypes = additionalTypes,
                requiredAdditionalExerciseTypes = additionalTypes,
            ),
        )
    }

    @Test
    fun sevenArithmeticSessionsDoNotNeedToBeSpreadAcrossSevenDaysForWeeklyChallenge() {
        // Unlike the streak, the challenge has a weekly arithmetic count requirement.
        assertTrue(
            WeeklyAchievementRules.isWeeklyChallengeCompleted(
                arithmeticSessionCount = 7,
                completedExerciseTypes = additionalTypes,
                requiredAdditionalExerciseTypes = additionalTypes,
            ),
        )
    }

    @Test
    fun additionalExerciseOutsideRequiredSetDoesNotSubstituteForMissingExercise() {
        assertFalse(
            WeeklyAchievementRules.isWeeklyChallengeCompleted(
                arithmeticSessionCount = 7,
                completedExerciseTypes = additionalTypes - "COUNTING_TRAINING" + "UNKNOWN",
                requiredAdditionalExerciseTypes = additionalTypes,
            ),
        )
    }

    @Test
    fun completingOnlyArithmeticDoesNotGrantWeeklyChallenge() {
        assertFalse(
            WeeklyAchievementRules.isWeeklyChallengeCompleted(
                arithmeticSessionCount = 10,
                completedExerciseTypes = emptySet(),
                requiredAdditionalExerciseTypes = additionalTypes,
            ),
        )
    }

    @Test
    fun streakRewardsRepeatAfterEachFurtherSevenConsecutiveArithmeticDays() {
        val fourteenDays = (0..13).map { weekStart.plusDays(it.toLong()) }

        assertEquals(
            listOf(weekStart.plusDays(6), weekStart.plusDays(13)),
            WeeklyAchievementRules.weeklyStreakCompletionDates(fourteenDays),
        )
    }
}

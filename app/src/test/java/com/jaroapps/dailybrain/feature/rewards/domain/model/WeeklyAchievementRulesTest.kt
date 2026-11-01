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
    fun weeklyChallengeRequiresArithmeticEveryDayAndOnlyOneAdditionalExercise() {
        assertFalse(
            WeeklyAchievementRules.isWeeklyChallengeCompleted(
                weekStart = weekStart,
                arithmeticDates = weekDates,
                completedExerciseTypes = emptySet(),
                additionalExerciseTypes = additionalTypes,
            ),
        )

        assertTrue(
            WeeklyAchievementRules.isWeeklyChallengeCompleted(
                weekStart = weekStart,
                arithmeticDates = weekDates,
                completedExerciseTypes = setOf("MEMORY_WORDS"),
                additionalExerciseTypes = additionalTypes,
            ),
        )
    }

    @Test
    fun completingSecondAndThirdAdditionalExercisesDoesNotRevokeEligibility() {
        val first = setOf("MEMORY_WORDS")
        val second = first + "STROOP_TEST"
        val allThree = second + "COUNTING_TRAINING"

        listOf(first, second, allThree).forEach { completed ->
            assertTrue(
                WeeklyAchievementRules.isWeeklyChallengeCompleted(
                    weekStart = weekStart,
                    arithmeticDates = weekDates,
                    completedExerciseTypes = completed,
                    additionalExerciseTypes = additionalTypes,
                ),
            )
        }
    }

    @Test
    fun arithmeticMissingFromAnyDayPreventsWeeklyChallenge() {
        assertFalse(
            WeeklyAchievementRules.isWeeklyChallengeCompleted(
                weekStart = weekStart,
                arithmeticDates = weekDates - weekStart.plusDays(2),
                completedExerciseTypes = setOf("MEMORY_WORDS", "STROOP_TEST", "COUNTING_TRAINING"),
                additionalExerciseTypes = additionalTypes,
            ),
        )
    }

    @Test
    fun additionalExerciseOutsideSelectedWeekDoesNotQualify() {
        assertFalse(
            WeeklyAchievementRules.isWeeklyChallengeCompleted(
                weekStart = weekStart,
                arithmeticDates = weekDates,
                completedExerciseTypes = emptySet(),
                additionalExerciseTypes = additionalTypes,
            ),
        )
    }
}

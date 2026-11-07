package com.jaroapps.dailybrain.feature.rewards.domain.model

import com.jaroapps.dailybrain.common.domain.model.ExerciseType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.YearMonth

class MonthlyChallengeRulesTest {

    @Test
    fun defaultMonthlyTargetsRepresentFourWeeksAndStayIndependentFromWeeklyRules() {
        assertEquals(
            mapOf(
                ExerciseType.ARITHMETIC.storageValue to 28,
                ExerciseType.MEMORY_WORDS.storageValue to 4,
                ExerciseType.STROOP_TEST.storageValue to 4,
                ExerciseType.COUNTING_TRAINING.storageValue to 4,
            ),
            MonthlyChallengeRules.DEFAULT_MONTHLY_CHALLENGE_REQUIREMENTS.associate {
                it.exerciseType to it.requiredCount
            },
        )
    }

    @Test
    fun monthlyChallengeCombinesVolumeAcrossWeeksWithoutRequiringWeeklyCompletion() {
        val fourWeeksOfSessions = buildList {
            repeat(28) { add(ExerciseType.ARITHMETIC.storageValue) }
            repeat(4) { add(ExerciseType.MEMORY_WORDS.storageValue) }
            repeat(4) { add(ExerciseType.STROOP_TEST.storageValue) }
            repeat(4) { add(ExerciseType.COUNTING_TRAINING.storageValue) }
        }
        assertTrue(MonthlyChallengeRules.isCompleted(fourWeeksOfSessions))
    }

    @Test
    fun missingAnyRequiredExerciseKeepsMonthlyChallengeIncomplete() {
        val sessions = buildList {
            repeat(28) { add(ExerciseType.ARITHMETIC.storageValue) }
            repeat(4) { add(ExerciseType.MEMORY_WORDS.storageValue) }
            repeat(4) { add(ExerciseType.STROOP_TEST.storageValue) }
            repeat(3) { add(ExerciseType.COUNTING_TRAINING.storageValue) }
        }
        assertFalse(MonthlyChallengeRules.isCompleted(sessions))
    }

    @Test
    fun progressCountsCompletedSessionsAndCapsAtEachMonthlyTarget() {
        val sessions = buildList {
            repeat(35) { add(ExerciseType.ARITHMETIC.storageValue) }
            repeat(2) { add(ExerciseType.MEMORY_WORDS.storageValue) }
        }
        assertEquals(
            mapOf(
                ExerciseType.ARITHMETIC.storageValue to 28,
                ExerciseType.MEMORY_WORDS.storageValue to 2,
                ExerciseType.STROOP_TEST.storageValue to 0,
                ExerciseType.COUNTING_TRAINING.storageValue to 0,
            ),
            MonthlyChallengeRules.progress(sessions),
        )
    }

    @Test
    fun completedMonthsAreReturnedInChronologicalOrder() {
        val jan = YearMonth.of(2026, 1)
        val feb = jan.plusMonths(1)
        val completed = List(28) { ExerciseType.ARITHMETIC.storageValue } +
            List(4) { ExerciseType.MEMORY_WORDS.storageValue } +
            List(4) { ExerciseType.STROOP_TEST.storageValue } +
            List(4) { ExerciseType.COUNTING_TRAINING.storageValue }
        assertEquals(
            listOf(jan, feb),
            MonthlyChallengeRules.completedMonths(mapOf(feb to completed, jan to completed)),
        )
    }
}

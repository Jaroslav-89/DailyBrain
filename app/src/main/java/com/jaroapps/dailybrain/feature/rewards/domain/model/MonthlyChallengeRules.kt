package com.jaroapps.dailybrain.feature.rewards.domain.model

import com.jaroapps.dailybrain.common.domain.model.ExerciseType
import java.time.YearMonth

/**
 * Independent four-week volume challenge for a calendar month.
 *
 * These targets intentionally do not derive from weekly challenge requirements: weekly tasks
 * may later be generated or changed without silently changing the monthly challenge.
 */
object MonthlyChallengeRules {

    const val XP_REWARD = 500

    data class ExerciseRequirement(
        val exerciseType: String,
        val requiredCount: Int,
    ) {
        init {
            require(exerciseType.isNotBlank())
            require(requiredCount > 0)
        }
    }

    val DEFAULT_MONTHLY_CHALLENGE_REQUIREMENTS = listOf(
        ExerciseRequirement(ExerciseType.ARITHMETIC.storageValue, 28),
        ExerciseRequirement(ExerciseType.MEMORY_WORDS.storageValue, 4),
        ExerciseRequirement(ExerciseType.STROOP_TEST.storageValue, 4),
        ExerciseRequirement(ExerciseType.COUNTING_TRAINING.storageValue, 4),
    )

    fun progress(
        completedExerciseTypes: Collection<String>,
        requirements: List<ExerciseRequirement> = DEFAULT_MONTHLY_CHALLENGE_REQUIREMENTS,
    ): Map<String, Int> {
        val counts = completedExerciseTypes.groupingBy { it }.eachCount()
        return requirements.associate { requirement ->
            requirement.exerciseType to minOf(
                counts[requirement.exerciseType] ?: 0,
                requirement.requiredCount,
            )
        }
    }

    fun isCompleted(
        completedExerciseTypes: Collection<String>,
        requirements: List<ExerciseRequirement> = DEFAULT_MONTHLY_CHALLENGE_REQUIREMENTS,
    ): Boolean {
        if (requirements.isEmpty()) return false
        val counts = completedExerciseTypes.groupingBy { it }.eachCount()
        return requirements.all { requirement ->
            (counts[requirement.exerciseType] ?: 0) >= requirement.requiredCount
        }
    }

    fun completedMonths(
        exerciseTypesByMonth: Map<YearMonth, Collection<String>>,
        requirements: List<ExerciseRequirement> = DEFAULT_MONTHLY_CHALLENGE_REQUIREMENTS,
    ): List<YearMonth> = exerciseTypesByMonth
        .filterValues { types -> isCompleted(types, requirements) }
        .keys
        .sorted()
}

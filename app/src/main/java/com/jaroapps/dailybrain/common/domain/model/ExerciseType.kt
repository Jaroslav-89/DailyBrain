package com.jaroapps.dailybrain.common.domain.model

enum class ExerciseType(
    val storageValue: String,
) {
    ARITHMETIC(storageValue = "ARITHMETIC"),
    MEMORY_WORDS(storageValue = "MEMORY_WORDS"),
    COUNTING_TRAINING(storageValue = "COUNTING_TRAINING"),
    STROOP_TEST(storageValue = "STROOP_TEST");

    companion object {
        fun fromStorage(value: String): ExerciseType? =
            entries.firstOrNull { it.storageValue == value }
    }
}

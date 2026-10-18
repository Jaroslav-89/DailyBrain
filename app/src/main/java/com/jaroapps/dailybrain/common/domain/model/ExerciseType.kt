package com.jaroapps.dailybrain.common.domain.model

enum class ExerciseType(
    val storageValue: String,
    val displayName: String,
) {
    ARITHMETIC(
        storageValue = "ARITHMETIC",
        displayName = "Арифметика",
    ),
    MEMORY_WORDS(
        storageValue = "MEMORY_WORDS",
        displayName = "Запоминание слов",
    ),
    COUNTING_TRAINING(
        storageValue = "COUNTING_TRAINING",
        displayName = "Устный счёт до 120",
    ),
    STROOP_TEST(
    storageValue = "STROOP_TEST",
    displayName = "Тест Струпа",
    );

    companion object {
        fun fromStorage(value: String): ExerciseType? =
            entries.firstOrNull { it.storageValue == value }
    }
}

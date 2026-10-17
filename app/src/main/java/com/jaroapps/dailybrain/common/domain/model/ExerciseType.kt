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
    );

    companion object {
        fun fromStorage(value: String): ExerciseType? =
            entries.firstOrNull { it.storageValue == value }
    }
}

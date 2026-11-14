package com.jaroapps.dailybrain.common.domain.model

enum class ExerciseType(
    val storageValue: String,
) {
    ARITHMETIC(storageValue = "ARITHMETIC"),
    MEMORY_WORDS(storageValue = "MEMORY_WORDS"),
    COUNTING_TRAINING(storageValue = "COUNTING_TRAINING"),
    STROOP_TEST(storageValue = "STROOP_TEST"),
    NUMBER_GRID(storageValue = "NUMBER_GRID");

    companion object {
        fun fromStorage(value: String): ExerciseType? =
            entries.firstOrNull { it.storageValue == value }
                ?: value.takeIf(::isNumberGridVariant)?.let { NUMBER_GRID }

        fun storageValueForGridSize(size: Int): String {
            require(size >= 3) { "A number grid must be at least 3×3" }
            return "NUMBER_GRID_${size}X${size}"
        }

        private fun isNumberGridVariant(value: String): Boolean {
            if (!value.startsWith("NUMBER_GRID_")) return false
            val dimensions = value.removePrefix("NUMBER_GRID_").split('X')
            if (dimensions.size != 2) return false
            val rows = dimensions[0].toIntOrNull() ?: return false
            val columns = dimensions[1].toIntOrNull() ?: return false
            return rows >= 3 && rows == columns
        }
    }
}

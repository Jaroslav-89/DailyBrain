package com.jaroapps.dailybrain.feature.stroop.domain.model

data class StroopStimulus(
    val word: String,
    val inkColor: StroopColor,
    val correctAnswer: StroopColor,
    val isCongruent: Boolean,
)

enum class StroopColor(
    val displayName: String,
    val argb: Long,
) {
    RED("КРАСНЫЙ", 0xFFE53935),
    BLUE("СИНИЙ", 0xFF1E88E5),
    YELLOW("ЖЁЛТЫЙ", 0xFFF9A825),
    GREEN("ЗЕЛЁНЫЙ", 0xFF43A047),
}
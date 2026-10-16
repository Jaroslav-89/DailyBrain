package com.jaroapps.dailybrain.feature.memory.domain.model

data class MemoryTrainingResult(
    val shownWords: List<MemoryWord>,
    val answers: List<MemoryAnswerResult>,
    val correctCount: Int,
    val accuracyPercent: Int
) {
    val missedWords: List<MemoryWord>
        get() = answers
            .filterNot { it.wasRecalled }
            .map { it.word }

    val recalledWords: List<MemoryWord>
        get() = answers
            .filter { it.wasRecalled }
            .map { it.word }
}
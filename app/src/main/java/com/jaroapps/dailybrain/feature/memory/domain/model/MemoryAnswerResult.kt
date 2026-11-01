package com.jaroapps.dailybrain.feature.memory.domain.model

data class MemoryAnswerResult(
    val word: MemoryWord,
    val wasRecalled: Boolean
)
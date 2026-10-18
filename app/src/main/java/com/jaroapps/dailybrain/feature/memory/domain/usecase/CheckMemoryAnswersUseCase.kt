package com.jaroapps.dailybrain.feature.memory.domain.usecase

import com.jaroapps.dailybrain.feature.memory.domain.model.MemoryAnswerResult
import com.jaroapps.dailybrain.feature.memory.domain.model.MemoryTrainingResult
import com.jaroapps.dailybrain.feature.memory.domain.model.MemoryWord
import java.util.Locale

class CheckMemoryAnswersUseCase {

    operator fun invoke(
        shownWords: List<MemoryWord>,
        enteredAnswers: List<String>
    ): MemoryTrainingResult {
        val normalizedAnswers = enteredAnswers
            .asSequence()
            .map(::normalize)
            .filter { it.isNotEmpty() }
            .toSet()

        val results = shownWords.map { word ->
            MemoryAnswerResult(
                word = word,
                wasRecalled = normalize(word.text) in normalizedAnswers
            )
        }

        val correctCount = results.count { it.wasRecalled }

        val accuracyPercent = if (shownWords.isEmpty()) {
            0
        } else {
            (correctCount * 100) / shownWords.size
        }

        return MemoryTrainingResult(
            shownWords = shownWords,
            answers = results,
            correctCount = correctCount,
            accuracyPercent = accuracyPercent
        )
    }

    private fun normalize(value: String): String {
        return value.trim().lowercase(Locale.ROOT)
    }
}
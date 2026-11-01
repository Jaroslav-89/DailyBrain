package com.jaroapps.dailybrain.feature.memory.domain.usecase

import com.jaroapps.dailybrain.feature.memory.domain.model.MemoryWord
import com.jaroapps.dailybrain.feature.memory.domain.repository.MemoryWordsRepository
import java.util.Locale
import kotlin.random.Random

class GetRandomMemoryWordsUseCase(
    private val repository: MemoryWordsRepository,
    private val random: Random = Random.Default
) {

    suspend operator fun invoke(count: Int = DEFAULT_WORD_COUNT): List<MemoryWord> {
        require(count > 0) {
            "Word count must be greater than zero"
        }

        val uniqueWords = repository.getAllWords()
            .map { it.copy(text = it.text.trim()) }
            .filter { it.text.isNotEmpty() }
            .distinctBy { it.text.lowercase(Locale.ROOT) }

        require(uniqueWords.size >= count) {
            "Not enough unique words: requested $count, available ${uniqueWords.size}"
        }

        return uniqueWords.shuffled(random).take(count)
    }

    private companion object {
        const val DEFAULT_WORD_COUNT = 30
    }
}
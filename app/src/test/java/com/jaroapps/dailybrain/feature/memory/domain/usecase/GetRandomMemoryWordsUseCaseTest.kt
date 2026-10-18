package com.jaroapps.dailybrain.feature.memory.domain.usecase

import com.jaroapps.dailybrain.feature.memory.domain.model.MemoryWord
import com.jaroapps.dailybrain.feature.memory.domain.repository.MemoryWordsRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class GetRandomMemoryWordsUseCaseTest {

    private class FakeRepository(
        private val words: List<MemoryWord>
    ) : MemoryWordsRepository {

        override suspend fun getAllWords(): List<MemoryWord> = words
    }

    @Test
    fun `returns requested number of distinct words`() {
        runBlocking {
            val words = (1..100).map { MemoryWord("Word $it") }
            val useCase = GetRandomMemoryWordsUseCase(
                repository = FakeRepository(words),
                random = Random(123)
            )

            val result = useCase(count = 30)

            assertEquals(30, result.size)
            assertEquals(30, result.map { it.text }.distinct().size)
        }
    }

    @Test
    fun `ignores duplicate words regardless of case and spaces`() {
        runBlocking {
            val useCase = GetRandomMemoryWordsUseCase(
                repository = FakeRepository(
                    listOf(
                        MemoryWord("Дом"),
                        MemoryWord(" дом "),
                        MemoryWord("Река"),
                        MemoryWord("Лес")
                    )
                )
            )

            val exception = try {
                useCase(count = 4)
                null
            } catch (e: IllegalArgumentException) {
                e
            }

            assertTrue(
                "Expected an exception because only three unique words exist",
                exception != null
            )
        }
    }

    @Test
    fun `throws when there are not enough unique words`() {
        runBlocking {
            val useCase = GetRandomMemoryWordsUseCase(
                repository = FakeRepository(
                    listOf(MemoryWord("Дом"), MemoryWord("Река"))
                )
            )

            val exception = try {
                useCase(count = 3)
                null
            } catch (e: IllegalArgumentException) {
                e
            }

            assertTrue(
                "Expected an exception when there are too few words",
                exception != null
            )
        }
    }

    @Test
    fun `trims words and removes empty entries`() {
        runBlocking {
            val useCase = GetRandomMemoryWordsUseCase(
                repository = FakeRepository(
                    listOf(
                        MemoryWord(" Дом "),
                        MemoryWord(""),
                        MemoryWord("Река"),
                        MemoryWord("Лес")
                    )
                )
            )

            val result = useCase(count = 3)

            assertTrue(result.all { it.text.isNotBlank() })
            assertTrue(result.any { it.text == "Дом" })
        }
    }
}

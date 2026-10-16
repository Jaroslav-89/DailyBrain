package com.jaroapps.dailybrain.feature.memory.domain.usecase

import com.jaroapps.dailybrain.feature.memory.domain.model.MemoryWord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CheckMemoryAnswersUseCaseTest {

    private val checkAnswers = CheckMemoryAnswersUseCase()

    private val words = listOf(
        MemoryWord("Яблоко"),
        MemoryWord("Дом"),
        MemoryWord("Река"),
        MemoryWord("Книга")
    )

    @Test
    fun `answers are checked regardless of order`() {
        val result = checkAnswers(
            shownWords = words,
            enteredAnswers = listOf("Книга", "Река", "Яблоко")
        )

        assertEquals(3, result.correctCount)
        assertEquals(75, result.accuracyPercent)
        assertEquals(
            listOf("Дом"),
            result.missedWords.map { it.text }
        )
    }

    @Test
    fun `answers ignore case and surrounding spaces`() {
        val result = checkAnswers(
            shownWords = words,
            enteredAnswers = listOf(
                "  ябЛОко ",
                "ДОМ",
                " река "
            )
        )

        assertEquals(3, result.correctCount)
    }

    @Test
    fun `duplicate answers do not increase score`() {
        val result = checkAnswers(
            shownWords = words,
            enteredAnswers = listOf(
                "Яблоко",
                "Яблоко",
                "ЯБЛОКО",
                "Дом"
            )
        )

        assertEquals(2, result.correctCount)
        assertEquals(50, result.accuracyPercent)
    }

    @Test
    fun `empty answers are treated as missed words`() {
        val result = checkAnswers(
            shownWords = words,
            enteredAnswers = listOf("", "   ")
        )

        assertEquals(0, result.correctCount)
        assertEquals(0, result.accuracyPercent)
        assertEquals(4, result.missedWords.size)
        assertTrue(result.recalledWords.isEmpty())
    }

    @Test
    fun `missed words preserve the original spelling`() {
        val result = checkAnswers(
            shownWords = listOf(MemoryWord("Солнце")),
            enteredAnswers = emptyList()
        )

        assertEquals("Солнце", result.missedWords.single().text)
    }

    @Test
    fun `empty word list produces zero accuracy`() {
        val result = checkAnswers(
            shownWords = emptyList(),
            enteredAnswers = listOf("Яблоко")
        )

        assertEquals(0, result.correctCount)
        assertEquals(0, result.accuracyPercent)
        assertTrue(result.answers.isEmpty())
    }
}
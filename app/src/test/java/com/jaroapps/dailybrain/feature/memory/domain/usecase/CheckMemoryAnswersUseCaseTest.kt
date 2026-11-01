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
        assertTrue(result.incorrectWords.isEmpty())
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
        assertTrue(result.incorrectWords.isEmpty())
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
        assertTrue(result.incorrectWords.isEmpty())
    }

    @Test
    fun `repeated correct answers are listed separately without increasing score`() {
        val result = checkAnswers(
            shownWords = words,
            enteredAnswers = listOf("Яблоко", "ЯБЛОКО", "Дом", " яблоко ")
        )

        assertEquals(2, result.correctCount)
        assertEquals(listOf("Яблоко", "Яблоко"), result.repeatedWords)
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
        assertTrue(result.incorrectWords.isEmpty())
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
    fun `incorrect answers are listed and preserve user spelling`() {
        val result = checkAnswers(
            shownWords = words,
            enteredAnswers = listOf(" яблоко ", "Дмо", "", "РЕКА", "Книгга")
        )

        assertEquals(listOf("Дмо", "Книгга"), result.incorrectWords)
        assertEquals(2, result.correctCount)
    }

    @Test
    fun `empty word list treats every non blank answer as incorrect`() {
        val result = checkAnswers(
            shownWords = emptyList(),
            enteredAnswers = listOf("Яблоко", "  ", "Дом")
        )

        assertEquals(0, result.correctCount)
        assertEquals(0, result.accuracyPercent)
        assertTrue(result.answers.isEmpty())
        assertEquals(listOf("Яблоко", "Дом"), result.incorrectWords)
    }
}
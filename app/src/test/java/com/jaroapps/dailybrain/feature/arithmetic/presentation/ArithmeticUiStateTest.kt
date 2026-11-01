package com.jaroapps.dailybrain.feature.arithmetic.presentation

import com.jaroapps.dailybrain.feature.arithmetic.domain.model.ArithmeticOperation
import com.jaroapps.dailybrain.feature.arithmetic.domain.model.ArithmeticProblem
import com.jaroapps.dailybrain.feature.arithmetic.domain.model.ArithmeticTaskResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ArithmeticUiStateTest {

    @Test
    fun `progress reflects completed tasks and total tasks`() {
        val state = ArithmeticUiState(totalTaskCount = 8, completedTaskCount = 3)

        assertEquals(0.375f, state.progress, 0.0001f)
    }

    @Test
    fun `current task number advances but never exceeds total`() {
        assertEquals(1, ArithmeticUiState(totalTaskCount = 10).currentTaskNumber)
        assertEquals(
            4,
            ArithmeticUiState(totalTaskCount = 10, completedTaskCount = 3).currentTaskNumber,
        )
        assertEquals(
            10,
            ArithmeticUiState(totalTaskCount = 10, completedTaskCount = 10).currentTaskNumber,
        )
    }

    @Test
    fun `incorrect answers includes only failed task results`() {
        val addition = ArithmeticProblem(2, 3, ArithmeticOperation.ADDITION)
        val subtraction = ArithmeticProblem(8, 3, ArithmeticOperation.SUBTRACTION)
        val correct = ArithmeticTaskResult(problem = addition, userAnswer = 5)
        val incorrect = ArithmeticTaskResult(problem = subtraction, userAnswer = 6)

        val state = ArithmeticUiState(taskResults = listOf(correct, incorrect))

        assertEquals(listOf(incorrect), state.incorrectAnswers)
        assertFalse(state.incorrectAnswers.contains(correct))
    }

    @Test
    fun `default state has no active training or results`() {
        val state = ArithmeticUiState()

        assertFalse(state.isStarted)
        assertFalse(state.isCompleted)
        assertEquals(100, state.totalTaskCount)
        assertEquals(0, state.completedTaskCount)
        assertTrue(state.taskResults.isEmpty())
        assertEquals(0f, state.progress, 0f)
    }
}

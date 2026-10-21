package com.jaroapps.dailybrain.feature.arithmetic.domain

import com.jaroapps.dailybrain.feature.arithmetic.domain.model.ArithmeticOperation
import com.jaroapps.dailybrain.feature.arithmetic.domain.model.ArithmeticProblem
import com.jaroapps.dailybrain.feature.arithmetic.domain.usecase.CheckArithmeticAnswerUseCase
import com.jaroapps.dailybrain.feature.arithmetic.domain.usecase.GenerateArithmeticProblemUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random
import com.jaroapps.dailybrain.feature.arithmetic.domain.model.ArithmeticTrainingConfig


class ArithmeticDomainTest {

    private val checker = CheckArithmeticAnswerUseCase()


    @Test
    fun `default training contains 100 tasks`() {
        assertEquals(
            100,
            ArithmeticTrainingConfig().taskCount,
        )
    }

    @Test
    fun `training task count can be customized`() {
        assertEquals(
            20,
            ArithmeticTrainingConfig(taskCount = 20).taskCount,
        )
    }

    @Test
    fun `addition operands and result stay within bounds`() {
        val generator = GenerateArithmeticProblemUseCase(
            random = Random(42),
        )

        repeat(1_000) {
            val problem = generator.execute(ArithmeticOperation.ADDITION)

            assertTrue(problem.firstOperand in 0..9)
            assertTrue(problem.secondOperand in 0..9)
            assertTrue(problem.correctAnswer in 0..18)
        }
    }

    @Test
    fun `subtraction follows operand and result bounds`() {
        val generator = GenerateArithmeticProblemUseCase(
            random = Random(42),
        )

        repeat(1_000) {
            val problem = generator.execute(ArithmeticOperation.SUBTRACTION)

            assertTrue(problem.firstOperand in 0..18)
            assertTrue(problem.secondOperand in 0..9)
            assertTrue(problem.correctAnswer in 0..18)
        }
    }

    @Test
    fun `multiplication operands and result stay within bounds`() {
        val generator = GenerateArithmeticProblemUseCase(
            random = Random(42),
        )

        repeat(1_000) {
            val problem = generator.execute(ArithmeticOperation.MULTIPLICATION)

            assertTrue(problem.firstOperand in 0..9)
            assertTrue(problem.secondOperand in 0..9)
            assertTrue(problem.correctAnswer in 0..81)
        }
    }

    @Test
    fun `addition calculates correct answer`() {
        val problem = ArithmeticProblem(
            firstOperand = 17,
            secondOperand = 25,
            operation = ArithmeticOperation.ADDITION,
        )

        assertEquals(42, problem.correctAnswer)
    }

    @Test
    fun `subtraction calculates correct answer`() {
        val problem = ArithmeticProblem(
            firstOperand = 42,
            secondOperand = 17,
            operation = ArithmeticOperation.SUBTRACTION,
        )

        assertEquals(25, problem.correctAnswer)
    }

    @Test
    fun `multiplication calculates correct answer`() {
        val problem = ArithmeticProblem(
            firstOperand = 7,
            secondOperand = 8,
            operation = ArithmeticOperation.MULTIPLICATION,
        )

        assertEquals(56, problem.correctAnswer)
    }

    @Test
    fun `checker accepts correct answer`() {
        val problem = ArithmeticProblem(
            firstOperand = 12,
            secondOperand = 15,
            operation = ArithmeticOperation.ADDITION,
        )

        assertTrue(checker.execute(problem, 27))
    }

    @Test
    fun `checker rejects incorrect answer`() {
        val problem = ArithmeticProblem(
            firstOperand = 12,
            secondOperand = 15,
            operation = ArithmeticOperation.ADDITION,
        )

        assertFalse(checker.execute(problem, 28))
    }

    @Test
    fun `subtraction generator never produces negative answer`() {
        val generator = GenerateArithmeticProblemUseCase(
            random = Random(42),
        )

        repeat(1_000) {
            val problem = generator.execute(
                ArithmeticOperation.SUBTRACTION,
            )

            assertTrue(problem.correctAnswer >= 0)
        }
    }

    @Test
    fun `generated answer matches the selected operation`() {
        val generator = GenerateArithmeticProblemUseCase(random = Random(7))

        ArithmeticOperation.values().forEach { operation ->
            repeat(250) {
                val problem = generator.execute(operation)
                val expected = when (operation) {
                    ArithmeticOperation.ADDITION -> problem.firstOperand + problem.secondOperand
                    ArithmeticOperation.SUBTRACTION -> problem.firstOperand - problem.secondOperand
                    ArithmeticOperation.MULTIPLICATION -> problem.firstOperand * problem.secondOperand
                }

                assertEquals("Incorrect result for $operation", expected, problem.correctAnswer)
            }
        }
    }

    @Test
    fun `checker rejects a neighboring incorrect answer for each operation`() {
        ArithmeticOperation.values().forEach { operation ->
            val problem = ArithmeticProblem(
                firstOperand = 8,
                secondOperand = 3,
                operation = operation,
            )

            assertFalse(
                "Checker accepted an incorrect answer for $operation",
                checker.execute(problem, problem.correctAnswer + 1),
            )
        }
    }
}

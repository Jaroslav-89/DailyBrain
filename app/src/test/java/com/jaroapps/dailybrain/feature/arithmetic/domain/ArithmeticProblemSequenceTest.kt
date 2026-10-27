package com.jaroapps.dailybrain.feature.arithmetic.domain

import com.jaroapps.dailybrain.feature.arithmetic.domain.model.ArithmeticOperation
import com.jaroapps.dailybrain.feature.arithmetic.domain.model.ArithmeticProblem
import com.jaroapps.dailybrain.feature.arithmetic.domain.usecase.GenerateArithmeticProblemUseCase
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ArithmeticProblemSequenceTest {
    @Test
    fun generatedProblemDiffersFromPreviousProblem() {
        val generator = GenerateArithmeticProblemUseCase()
        val previous = ArithmeticProblem(4, 2, ArithmeticOperation.ADDITION)

        repeat(100) {
            val next = generator.executeAvoiding(previous)
            assertNotEquals(previous, next)
        }
    }

    @Test
    fun sameProblemFallsBackToDifferentOperation() {
        val generator = GenerateArithmeticProblemUseCase()
        val previous = ArithmeticProblem(4, 2, ArithmeticOperation.ADDITION)
        val next = generator.executeAvoiding(previous)

        assertNotEquals(previous, next)
    }
}

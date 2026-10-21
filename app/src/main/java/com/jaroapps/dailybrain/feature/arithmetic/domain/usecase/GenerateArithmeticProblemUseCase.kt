package com.jaroapps.dailybrain.feature.arithmetic.domain.usecase

import com.jaroapps.dailybrain.feature.arithmetic.domain.model.ArithmeticOperation
import com.jaroapps.dailybrain.feature.arithmetic.domain.model.ArithmeticProblem
import kotlin.random.Random

class GenerateArithmeticProblemUseCase(
    private val random: Random = Random.Default,
) {

    fun execute(): ArithmeticProblem {
        val operation = ArithmeticOperation.values().random(random)
        return execute(operation)
    }

    fun execute(
        operation: ArithmeticOperation,
    ): ArithmeticProblem {
        return when (operation) {
            ArithmeticOperation.ADDITION -> {
                val first = nextOperand(maxInclusive = 9)
                val second = nextOperand(
                    maxInclusive = 9,
                    differentFrom = first,
                )
                ArithmeticProblem(
                    firstOperand = first,
                    secondOperand = second,
                    operation = operation,
                )
            }

            ArithmeticOperation.SUBTRACTION -> {
                // A zero first operand would force the second operand to zero too.
                val first = random.nextInt(1, 19)
                val second = nextOperand(
                    maxInclusive = minOf(first, 9),
                    differentFrom = first,
                )
                ArithmeticProblem(
                    firstOperand = first,
                    secondOperand = second,
                    operation = operation,
                )
            }

            ArithmeticOperation.MULTIPLICATION -> {
                val first = nextOperand(maxInclusive = 9)
                val second = nextOperand(
                    maxInclusive = 9,
                    differentFrom = first,
                )
                ArithmeticProblem(
                    firstOperand = first,
                    secondOperand = second,
                    operation = operation,
                )
            }
        }
    }

    /**
     * Zero is deliberately rare. Equal operands remain possible, but are
     * retried most of the time to make the generated examples more varied.
     */
    private fun nextOperand(
        maxInclusive: Int,
        differentFrom: Int? = null,
    ): Int {
        require(maxInclusive >= 1)

        var candidate = randomOperand(maxInclusive)
        if (candidate == differentFrom && random.nextInt(100) < EQUAL_OPERAND_RETRY_PERCENT) {
            candidate = randomOperand(maxInclusive)
            while (candidate == differentFrom) {
                candidate = randomOperand(maxInclusive)
            }
        }

        // Never produce examples where both operands are zero.
        if (candidate == 0 && differentFrom == 0) {
            candidate = random.nextInt(1, maxInclusive + 1)
        }

        return candidate
    }

    private fun randomOperand(maxInclusive: Int): Int {
        return if (random.nextInt(100) < ZERO_OPERAND_PERCENT) {
            0
        } else {
            random.nextInt(1, maxInclusive + 1)
        }
    }

    private companion object {
        const val ZERO_OPERAND_PERCENT = 5
        const val EQUAL_OPERAND_RETRY_PERCENT = 80
    }
}

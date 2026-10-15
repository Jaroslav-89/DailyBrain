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
                ArithmeticProblem(
                    firstOperand = random.nextInt(0, 10),
                    secondOperand = random.nextInt(0, 10),
                    operation = operation,
                )
            }

            ArithmeticOperation.SUBTRACTION -> {
                val first = random.nextInt(0, 19)
                val second = random.nextInt(0, minOf(first, 9) + 1)

                ArithmeticProblem(
                    firstOperand = first,
                    secondOperand = second,
                    operation = operation,
                )
            }

            ArithmeticOperation.MULTIPLICATION -> {
                ArithmeticProblem(
                    firstOperand = random.nextInt(0, 10),
                    secondOperand = random.nextInt(0, 10),
                    operation = operation,
                )
            }
        }
    }
}
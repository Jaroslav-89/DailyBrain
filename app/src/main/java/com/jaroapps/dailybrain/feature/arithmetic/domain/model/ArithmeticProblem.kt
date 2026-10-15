package com.jaroapps.dailybrain.feature.arithmetic.domain.model

data class ArithmeticProblem(
    val firstOperand: Int,
    val secondOperand: Int,
    val operation: ArithmeticOperation,
) {
    val correctAnswer: Int
        get() = when (operation) {
            ArithmeticOperation.ADDITION ->
                firstOperand + secondOperand

            ArithmeticOperation.SUBTRACTION ->
                firstOperand - secondOperand

            ArithmeticOperation.MULTIPLICATION ->
                firstOperand * secondOperand
        }
}
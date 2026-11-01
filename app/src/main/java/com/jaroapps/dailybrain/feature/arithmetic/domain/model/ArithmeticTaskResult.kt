package com.jaroapps.dailybrain.feature.arithmetic.domain.model

data class ArithmeticTaskResult(
    val problem: ArithmeticProblem,
    val userAnswer: Int,
) {
    val correctAnswer: Int
        get() = problem.correctAnswer

    val isCorrect: Boolean
        get() = userAnswer == correctAnswer
}
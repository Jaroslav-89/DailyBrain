package com.jaroapps.dailybrain.feature.arithmetic.domain.usecase

import com.jaroapps.dailybrain.feature.arithmetic.domain.model.ArithmeticProblem

class CheckArithmeticAnswerUseCase {

    fun execute(
        problem: ArithmeticProblem,
        userAnswer: Int,
    ): Boolean {
        return problem.correctAnswer == userAnswer
    }
}
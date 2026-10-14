package com.jaroapps.dailybrain.app.di

import org.koin.dsl.module
import com.jaroapps.dailybrain.feature.arithmetic.domain.usecase.CheckArithmeticAnswerUseCase
import com.jaroapps.dailybrain.feature.arithmetic.domain.usecase.GenerateArithmeticProblemUseCase
import com.jaroapps.dailybrain.feature.arithmetic.presentation.ArithmeticViewModel
import org.koin.core.module.dsl.viewModel

val appModule = module {

    factory {
        GenerateArithmeticProblemUseCase()
    }

    factory {
        CheckArithmeticAnswerUseCase()
    }

    viewModel {
        ArithmeticViewModel(
            generateProblem = get(),
            checkAnswer = get(),
        )
    }
}
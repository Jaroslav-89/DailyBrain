package com.jaroapps.dailybrain.app.di

import org.koin.dsl.module
import com.jaroapps.dailybrain.feature.arithmetic.domain.usecase.CheckArithmeticAnswerUseCase
import com.jaroapps.dailybrain.feature.arithmetic.domain.usecase.GenerateArithmeticProblemUseCase
import com.jaroapps.dailybrain.feature.arithmetic.presentation.ArithmeticViewModel
import org.koin.core.module.dsl.viewModel
import androidx.room.Room
import com.jaroapps.dailybrain.core.database.AppDatabase
import com.jaroapps.dailybrain.feature.history.data.TrainingSessionRepositoryImpl
import com.jaroapps.dailybrain.feature.history.domain.repository.TrainingSessionRepository
import org.koin.android.ext.koin.androidContext

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
            trainingSessionRepository = get(),
        )
    }

    single {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            "daily_brain.db",
        )
            .fallbackToDestructiveMigration(true)
            .build()
    }

    single {
        get<AppDatabase>().trainingSessionDao()
    }

    single<TrainingSessionRepository> {
        TrainingSessionRepositoryImpl(
            dao = get(),
        )
    }
}
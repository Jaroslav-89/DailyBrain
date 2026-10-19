package com.jaroapps.dailybrain.app.di

import androidx.room.Room
import com.jaroapps.dailybrain.common.data.repository.CompletedTrainingRepositoryImpl
import com.jaroapps.dailybrain.common.domain.repository.CompletedTrainingRepository
import com.jaroapps.dailybrain.common.domain.usecase.ObserveActiveTrainingDatesUseCase
import com.jaroapps.dailybrain.common.domain.usecase.ObserveAllCompletedTrainingsUseCase
import com.jaroapps.dailybrain.common.domain.usecase.ObserveCompletedTrainingsByDateUseCase
import com.jaroapps.dailybrain.core.database.AppDatabase
import com.jaroapps.dailybrain.feature.arithmetic.domain.usecase.CheckArithmeticAnswerUseCase
import com.jaroapps.dailybrain.feature.arithmetic.domain.usecase.GenerateArithmeticProblemUseCase
import com.jaroapps.dailybrain.feature.arithmetic.domain.usecase.SaveArithmeticTrainingUseCase
import com.jaroapps.dailybrain.feature.arithmetic.presentation.ArithmeticViewModel
import com.jaroapps.dailybrain.feature.counting.domain.usecase.ObserveCountingStatisticsUseCase
import com.jaroapps.dailybrain.feature.counting.domain.usecase.SaveCountingTrainingUseCase
import com.jaroapps.dailybrain.feature.counting.presentation.CountingStatisticsViewModel
import com.jaroapps.dailybrain.feature.counting.presentation.CountingTrainingViewModel
import com.jaroapps.dailybrain.feature.history.presentation.CalendarViewModel
import com.jaroapps.dailybrain.feature.history.presentation.DayHistoryViewModel
import com.jaroapps.dailybrain.feature.home.presentation.HomeViewModel
import com.jaroapps.dailybrain.feature.memory.data.MemoryWordsRepositoryImpl
import com.jaroapps.dailybrain.feature.memory.domain.repository.MemoryWordsRepository
import com.jaroapps.dailybrain.feature.memory.domain.usecase.CheckMemoryAnswersUseCase
import com.jaroapps.dailybrain.feature.memory.domain.usecase.GetRandomMemoryWordsUseCase
import com.jaroapps.dailybrain.feature.memory.domain.usecase.SaveMemoryTrainingUseCase
import com.jaroapps.dailybrain.feature.memory.presentation.MemoryTrainingViewModel
import com.jaroapps.dailybrain.feature.statistics.presentation.StatisticsViewModel
import com.jaroapps.dailybrain.feature.stroop.domain.usecase.GenerateStroopStimuliUseCase
import com.jaroapps.dailybrain.feature.stroop.domain.usecase.SaveStroopTrainingUseCase
import com.jaroapps.dailybrain.feature.stroop.presentation.StroopViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {

    // Common data and use cases
    single {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            "daily_brain.db",
        )
            // Не удаляем историю пользователя автоматически при несовместимой схеме.
            // При изменении схемы нужно добавить явную Room Migration.
            .build()
    }

    single<CompletedTrainingRepository> {
        CompletedTrainingRepositoryImpl(database = get())
    }

    factory { ObserveAllCompletedTrainingsUseCase(repository = get()) }
    factory { ObserveCompletedTrainingsByDateUseCase(repository = get()) }
    factory { ObserveActiveTrainingDatesUseCase(repository = get()) }

    // Arithmetic feature
    factory { GenerateArithmeticProblemUseCase() }
    factory { CheckArithmeticAnswerUseCase() }
    factory { SaveArithmeticTrainingUseCase(completedTrainingRepository = get()) }

    viewModel {
        ArithmeticViewModel(
            generateProblem = get(),
            checkAnswer = get(),
            saveArithmeticTraining = get(),
        )
    }

    // Memory feature
    single<MemoryWordsRepository> {
        MemoryWordsRepositoryImpl(context = androidContext())
    }

    factory { GetRandomMemoryWordsUseCase(repository = get()) }
    factory { CheckMemoryAnswersUseCase() }
    factory { SaveMemoryTrainingUseCase(completedTrainingRepository = get()) }

    viewModel {
        MemoryTrainingViewModel(
            getRandomMemoryWords = get(),
            checkMemoryAnswers = get(),
            saveMemoryTraining = get(),
        )
    }

    // Counting feature
    factory { SaveCountingTrainingUseCase(completedTrainingRepository = get()) }
    factory { ObserveCountingStatisticsUseCase(completedTrainingRepository = get()) }

    viewModel { CountingTrainingViewModel(saveCountingTraining = get()) }
    viewModel { CountingStatisticsViewModel(observeCountingStatistics = get()) }

    // Stroop feature
    factory { GenerateStroopStimuliUseCase() }
    factory { SaveStroopTrainingUseCase(completedTrainingRepository = get()) }

    viewModel {
        StroopViewModel(
            generateStimuli = get(),
            saveStroopTraining = get(),
        )
    }

    // History feature
    viewModel { CalendarViewModel(observeActiveTrainingDates = get()) }

    viewModel {
        DayHistoryViewModel(
            savedStateHandle = get(),
            observeCompletedTrainingsByDate = get(),
        )
    }

    // Home feature
    viewModel { HomeViewModel(observeAllCompletedTrainings = get()) }

    // Statistics feature
    viewModel { StatisticsViewModel(observeAllCompletedTrainings = get()) }
}

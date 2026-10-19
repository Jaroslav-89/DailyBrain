package com.jaroapps.dailybrain.app.di

import androidx.room.Room
import com.jaroapps.dailybrain.common.data.repository.CompletedTrainingRepositoryImpl
import com.jaroapps.dailybrain.common.domain.repository.CompletedTrainingRepository
import com.jaroapps.dailybrain.common.domain.usecase.ObserveAllCompletedTrainingsUseCase
import com.jaroapps.dailybrain.common.domain.usecase.ObserveCompletedTrainingsByDateUseCase
import com.jaroapps.dailybrain.common.domain.usecase.SaveCompletedTrainingUseCase
import com.jaroapps.dailybrain.core.database.AppDatabase
import com.jaroapps.dailybrain.feature.arithmetic.domain.usecase.CheckArithmeticAnswerUseCase
import com.jaroapps.dailybrain.feature.arithmetic.domain.usecase.GenerateArithmeticProblemUseCase
import com.jaroapps.dailybrain.feature.arithmetic.presentation.ArithmeticViewModel
import com.jaroapps.dailybrain.feature.counting.domain.usecase.ObserveCountingStatisticsUseCase
import com.jaroapps.dailybrain.feature.counting.presentation.CountingStatisticsViewModel
import com.jaroapps.dailybrain.feature.counting.presentation.CountingTrainingViewModel
import com.jaroapps.dailybrain.feature.history.data.TrainingCalendarRepositoryImpl
import com.jaroapps.dailybrain.feature.history.domain.repository.TrainingCalendarRepository
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
import com.jaroapps.dailybrain.feature.stroop.presentation.StroopViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {

    // Common data layer
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

    single { get<AppDatabase>().trainingSessionDao() }
    single { get<AppDatabase>().trainingDayDao() }

    single<CompletedTrainingRepository> {
        CompletedTrainingRepositoryImpl(database = get())
    }

    // Common domain use cases
    factory { SaveCompletedTrainingUseCase(repository = get()) }
    factory { ObserveAllCompletedTrainingsUseCase(repository = get()) }
    factory { ObserveCompletedTrainingsByDateUseCase(repository = get()) }

    // Arithmetic feature
    factory { GenerateArithmeticProblemUseCase() }
    factory { CheckArithmeticAnswerUseCase() }

    viewModel {
        ArithmeticViewModel(
            generateProblem = get(),
            checkAnswer = get(),
            saveCompletedTraining = get(),
        )
    }

    // Counting feature
    factory { ObserveCountingStatisticsUseCase(completedTrainingRepository = get()) }

    viewModel {
        CountingTrainingViewModel(saveCompletedTraining = get())
    }

    viewModel {
        CountingStatisticsViewModel(observeCountingStatistics = get())
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

    // Stroop feature
    factory { GenerateStroopStimuliUseCase() }

    viewModel {
        StroopViewModel(
            generateStimuli = get(),
            saveCompletedTraining = get(),
        )
    }

    // Home feature
    viewModel {
        HomeViewModel(observeAllCompletedTrainings = get())
    }

    // Statistics feature
    viewModel {
        StatisticsViewModel(observeAllCompletedTrainings = get())
    }

    // History feature
    single<TrainingCalendarRepository> {
        TrainingCalendarRepositoryImpl(dao = get())
    }

    viewModel {
        CalendarViewModel(repository = get())
    }

    viewModel {
        DayHistoryViewModel(
            savedStateHandle = get(),
            observeCompletedTrainingsByDate = get(),
        )
    }
}

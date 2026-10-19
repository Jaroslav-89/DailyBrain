package com.jaroapps.dailybrain.app.di

import androidx.room.Room
import com.jaroapps.dailybrain.common.data.repository.CompletedTrainingRepositoryImpl
import com.jaroapps.dailybrain.common.domain.repository.CompletedTrainingRepository
import com.jaroapps.dailybrain.core.database.AppDatabase
import com.jaroapps.dailybrain.feature.arithmetic.domain.usecase.CheckArithmeticAnswerUseCase
import com.jaroapps.dailybrain.feature.arithmetic.domain.usecase.GenerateArithmeticProblemUseCase
import com.jaroapps.dailybrain.feature.arithmetic.presentation.ArithmeticViewModel
import com.jaroapps.dailybrain.feature.counting.data.repository.CountingTrainingRepositoryImpl
import com.jaroapps.dailybrain.feature.counting.domain.repository.CountingTrainingRepository
import com.jaroapps.dailybrain.feature.counting.presentation.CountingStatisticsViewModel
import com.jaroapps.dailybrain.feature.counting.presentation.CountingTrainingViewModel
import com.jaroapps.dailybrain.feature.history.data.HistoryRepositoryImpl
import com.jaroapps.dailybrain.feature.history.data.TrainingCalendarRepositoryImpl
import com.jaroapps.dailybrain.feature.history.domain.repository.HistoryRepository
import com.jaroapps.dailybrain.feature.history.domain.repository.TrainingCalendarRepository
import com.jaroapps.dailybrain.feature.history.presentation.CalendarViewModel
import com.jaroapps.dailybrain.feature.history.presentation.DayHistoryViewModel
import com.jaroapps.dailybrain.feature.home.presentation.HomeViewModel
import com.jaroapps.dailybrain.feature.memory.data.MemoryWordsRepositoryImpl
import com.jaroapps.dailybrain.feature.memory.domain.repository.MemoryWordsRepository
import com.jaroapps.dailybrain.feature.memory.domain.usecase.GetRandomMemoryWordsUseCase
import com.jaroapps.dailybrain.feature.memory.domain.usecase.SaveMemoryTrainingUseCase
import com.jaroapps.dailybrain.feature.statistics.presentation.StatisticsViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import com.jaroapps.dailybrain.feature.memory.domain.usecase.CheckMemoryAnswersUseCase
import com.jaroapps.dailybrain.feature.memory.presentation.MemoryTrainingViewModel
import com.jaroapps.dailybrain.feature.stroop.domain.usecase.GenerateStroopStimuliUseCase
import com.jaroapps.dailybrain.feature.stroop.presentation.StroopViewModel

val appModule = module {
//разбить на фичи, либо на отдельные модули для каждой фичи
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
            completedTrainingRepository = get(),
        )
    }

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

    single {
        get<AppDatabase>().trainingSessionDao()
    }

    single<HistoryRepository> {
        HistoryRepositoryImpl(
            dao = get(),
        )
    }

    viewModel {
        HomeViewModel(
            repository = get(),
        )
    }

    viewModel {
        StatisticsViewModel(
            repository = get(),
        )
    }

    single<TrainingCalendarRepository> {
        TrainingCalendarRepositoryImpl(
            dao = get(),
        )
    }

    single { get<AppDatabase>().trainingDayDao() }

    single<CompletedTrainingRepository> {
        CompletedTrainingRepositoryImpl(
            database = get(),
        )
    }

    viewModel {
        CalendarViewModel(
            repository = get(),
        )
    }

    viewModel {
        DayHistoryViewModel(
            savedStateHandle = get(),
            repository = get(),
        )
    }

    factory {
        SaveMemoryTrainingUseCase(
            completedTrainingRepository = get()
        )
    }

    single<MemoryWordsRepository> {
        MemoryWordsRepositoryImpl(
            context = androidContext()
        )
    }

    factory {
        GetRandomMemoryWordsUseCase(
            repository = get()
        )
    }

    factory {
        CheckMemoryAnswersUseCase()
    }

    viewModel {
        MemoryTrainingViewModel(
            getRandomMemoryWords = get(),
            checkMemoryAnswers = get(),
            saveMemoryTraining = get()
        )
    }

    factory {
        GenerateStroopStimuliUseCase()
    }

    viewModel {
        StroopViewModel(
            generateStimuli = get(),
            completedTrainingRepository = get(),
        )
    }

    single<CountingTrainingRepository> {
        CountingTrainingRepositoryImpl(
            database = get(),
        )
    }

    viewModel {
        CountingTrainingViewModel(
            completedTrainingRepository = get(),
        )
    }

    viewModel {
        CountingStatisticsViewModel(
            repository = get(),
        )
    }
}
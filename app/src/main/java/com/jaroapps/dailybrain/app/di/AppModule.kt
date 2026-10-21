package com.jaroapps.dailybrain.app.di

import androidx.room.Room
import com.jaroapps.dailybrain.common.data.repository.CompletedTrainingRepositoryImpl
import com.jaroapps.dailybrain.common.domain.repository.CompletedTrainingRepository
import com.jaroapps.dailybrain.common.domain.usecase.DeleteCompletedTrainingUseCase
import com.jaroapps.dailybrain.common.domain.usecase.ObserveActiveTrainingDatesUseCase
import com.jaroapps.dailybrain.common.domain.usecase.ObserveAllCompletedTrainingsUseCase
import com.jaroapps.dailybrain.common.domain.usecase.ObserveCompletedTrainingsByDateUseCase
import com.jaroapps.dailybrain.common.domain.usecase.ObserveTotalTrainingCountUseCase
import com.jaroapps.dailybrain.common.domain.usecase.ObserveTrainingDaysInRangeUseCase
import com.jaroapps.dailybrain.common.domain.usecase.SaveCompletedTrainingUseCase
import com.jaroapps.dailybrain.core.database.AppDatabase
import com.jaroapps.dailybrain.core.database.ActiveUserProvider
import com.jaroapps.dailybrain.core.database.DatabaseMigrations
import com.jaroapps.dailybrain.core.resources.AndroidStringProvider
import com.jaroapps.dailybrain.core.resources.StringProvider
import com.jaroapps.dailybrain.feature.avatar.data.AvatarRepositoryImpl
import com.jaroapps.dailybrain.feature.avatar.domain.repository.AvatarRepository
import com.jaroapps.dailybrain.feature.avatar.domain.usecase.ObserveAvatarUseCase
import com.jaroapps.dailybrain.feature.avatar.domain.usecase.ResetAvatarUseCase
import com.jaroapps.dailybrain.feature.avatar.domain.usecase.SaveAvatarUseCase
import com.jaroapps.dailybrain.feature.avatar.presentation.AvatarViewModel
import com.jaroapps.dailybrain.feature.arithmetic.domain.usecase.CheckArithmeticAnswerUseCase
import com.jaroapps.dailybrain.feature.arithmetic.domain.usecase.GenerateArithmeticProblemUseCase
import com.jaroapps.dailybrain.feature.arithmetic.presentation.ArithmeticViewModel
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
import com.jaroapps.dailybrain.feature.statistics.data.repository.StatisticsRepositoryImpl
import com.jaroapps.dailybrain.feature.statistics.domain.repository.StatisticsRepository
import com.jaroapps.dailybrain.feature.statistics.domain.usecase.ObserveStatisticsUseCase
import com.jaroapps.dailybrain.feature.statistics.presentation.StatisticsViewModel
import com.jaroapps.dailybrain.feature.profile.data.repository.UserProfileRepositoryImpl
import com.jaroapps.dailybrain.feature.profile.data.repository.ProfileStatisticsRepositoryImpl
import com.jaroapps.dailybrain.feature.profile.domain.repository.ProfileStatisticsRepository
import com.jaroapps.dailybrain.feature.profile.domain.repository.UserProfileRepository
import com.jaroapps.dailybrain.feature.profile.domain.usecase.ObserveUserProfileUseCase
import com.jaroapps.dailybrain.feature.profile.domain.usecase.ObserveLocalProfilesUseCase
import com.jaroapps.dailybrain.feature.profile.domain.usecase.CreateLocalProfileUseCase
import com.jaroapps.dailybrain.feature.profile.domain.usecase.SwitchActiveProfileUseCase
import com.jaroapps.dailybrain.feature.profile.domain.usecase.DeleteActiveProfileUseCase
import com.jaroapps.dailybrain.feature.profile.domain.usecase.SaveUserProfileUseCase
import com.jaroapps.dailybrain.feature.profile.domain.usecase.ObserveProfileStatisticsUseCase
import com.jaroapps.dailybrain.feature.profile.presentation.ProfileViewModel
import com.jaroapps.dailybrain.feature.stroop.domain.usecase.GenerateStroopStimuliUseCase
import com.jaroapps.dailybrain.feature.stroop.presentation.StroopViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {

    single<StringProvider> { AndroidStringProvider(androidContext()) }

    // Common data and use cases
    single {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            "daily_brain.db",
        )
            .addMigrations(DatabaseMigrations.MIGRATION_1_2, DatabaseMigrations.MIGRATION_2_3, DatabaseMigrations.MIGRATION_3_4, DatabaseMigrations.MIGRATION_4_5, DatabaseMigrations.MIGRATION_5_6, DatabaseMigrations.MIGRATION_6_7, DatabaseMigrations.MIGRATION_7_8)
            .build()
    }

    single { ActiveUserProvider(database = get()) }

    single<CompletedTrainingRepository> {
        CompletedTrainingRepositoryImpl(database = get(), activeUserProvider = get())
    }

    factory { ObserveAllCompletedTrainingsUseCase(repository = get()) }
    factory { ObserveCompletedTrainingsByDateUseCase(repository = get()) }
    factory { ObserveActiveTrainingDatesUseCase(repository = get()) }
    factory { ObserveTrainingDaysInRangeUseCase(repository = get()) }
    factory { ObserveTotalTrainingCountUseCase(repository = get()) }
    factory { SaveCompletedTrainingUseCase(repository = get()) }
    factory { DeleteCompletedTrainingUseCase(repository = get()) }

    // Local profile/account feature
    single<UserProfileRepository> { UserProfileRepositoryImpl(database = get(), activeUserProvider = get()) }
    factory { ObserveUserProfileUseCase(repository = get()) }
    factory { ObserveLocalProfilesUseCase(repository = get()) }
    factory { CreateLocalProfileUseCase(repository = get()) }
    factory { SwitchActiveProfileUseCase(repository = get()) }
    factory { DeleteActiveProfileUseCase(repository = get()) }
    factory { SaveUserProfileUseCase(repository = get()) }
    single<ProfileStatisticsRepository> { ProfileStatisticsRepositoryImpl(database = get(), activeUserProvider = get()) }
    factory { ObserveProfileStatisticsUseCase(repository = get()) }
    viewModel { ProfileViewModel(observeUserProfile = get(), observeProfileStatistics = get(), saveUserProfile = get(), observeLocalProfiles = get(), createLocalProfile = get(), switchActiveProfile = get(), deleteActiveProfile = get()) }

    // Local avatar feature: domain use cases depend on the repository contract.
    single<AvatarRepository> { AvatarRepositoryImpl(userProfileDao = get<AppDatabase>().userProfileDao(), activeUserProvider = get()) }
    factory { ObserveAvatarUseCase(repository = get()) }
    factory { SaveAvatarUseCase(repository = get()) }
    factory { ResetAvatarUseCase(repository = get()) }
    viewModel { AvatarViewModel(observeAvatar = get(), saveAvatar = get(), resetAvatar = get()) }

    single<StatisticsRepository> { StatisticsRepositoryImpl(database = get(), activeUserProvider = get()) }
    factory { ObserveStatisticsUseCase(repository = get()) }

    // Arithmetic feature
    factory { GenerateArithmeticProblemUseCase() }
    factory { CheckArithmeticAnswerUseCase() }
    viewModel {
        ArithmeticViewModel(
            generateProblem = get(),
            checkAnswer = get(),
            saveCompletedTraining = get(),
            strings = get(),
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
            strings = get(),
        )
    }

    // Counting feature
    viewModel { CountingTrainingViewModel(saveCompletedTraining = get(), strings = get()) }

    // Stroop feature
    factory { GenerateStroopStimuliUseCase() }

    viewModel {
        StroopViewModel(
            generateStimuli = get(),
            saveCompletedTraining = get(),
            strings = get(),
        )
    }

    // History
    viewModel { CalendarViewModel(observeActiveTrainingDates = get()) }

    viewModel {
        DayHistoryViewModel(
            savedStateHandle = get(),
            observeCompletedTrainingsByDate = get(),
            deleteCompletedTraining = get(),
        )
    }

    viewModel { HomeViewModel(observeTrainingDaysInRange = get(), observeTotalTrainingCount = get()) }
    viewModel { StatisticsViewModel(observeAllCompletedTrainings = get(), observeStatistics = get()) }
}

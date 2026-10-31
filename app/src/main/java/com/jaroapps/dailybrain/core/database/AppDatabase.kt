package com.jaroapps.dailybrain.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.jaroapps.dailybrain.core.database.dao.DailyExerciseResultDao
import com.jaroapps.dailybrain.core.database.dao.TrainingDayDao
import com.jaroapps.dailybrain.core.database.dao.TrainingSessionDao
import com.jaroapps.dailybrain.core.database.dao.StatisticsDao
import com.jaroapps.dailybrain.core.database.dao.UserProfileDao
import com.jaroapps.dailybrain.core.database.dao.OnboardingProgressDao
import com.jaroapps.dailybrain.core.database.entity.DailyExerciseResultEntity
import com.jaroapps.dailybrain.core.database.entity.TrainingDayEntity
import com.jaroapps.dailybrain.core.database.entity.TrainingSessionEntity
import com.jaroapps.dailybrain.core.database.entity.OverallStatisticsEntity
import com.jaroapps.dailybrain.core.database.entity.ExerciseStatisticsEntity
import com.jaroapps.dailybrain.core.database.entity.UserProfileEntity
import com.jaroapps.dailybrain.core.database.entity.AppSettingsEntity
import com.jaroapps.dailybrain.core.database.entity.OnboardingProgressEntity

@Database(
    entities = [
        TrainingSessionEntity::class,
        TrainingDayEntity::class,
        OverallStatisticsEntity::class,
        ExerciseStatisticsEntity::class,
        DailyExerciseResultEntity::class,
        UserProfileEntity::class,
        AppSettingsEntity::class,
        OnboardingProgressEntity::class,
    ],
    version = 10,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun trainingSessionDao(): TrainingSessionDao
    abstract fun trainingDayDao(): TrainingDayDao
    abstract fun statisticsDao(): StatisticsDao
    abstract fun dailyExerciseResultDao(): DailyExerciseResultDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun appSettingsDao(): com.jaroapps.dailybrain.core.database.dao.AppSettingsDao
    abstract fun onboardingProgressDao(): OnboardingProgressDao
}

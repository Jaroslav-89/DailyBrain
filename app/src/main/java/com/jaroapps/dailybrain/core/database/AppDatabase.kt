package com.jaroapps.dailybrain.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.jaroapps.dailybrain.core.database.dao.DailyExerciseResultDao
import com.jaroapps.dailybrain.core.database.dao.TrainingDayDao
import com.jaroapps.dailybrain.core.database.dao.TrainingSessionDao
import com.jaroapps.dailybrain.core.database.dao.StatisticsDao
import com.jaroapps.dailybrain.core.database.dao.UserProfileDao
import com.jaroapps.dailybrain.core.database.dao.RewardLedgerDao
import com.jaroapps.dailybrain.core.database.dao.UserBadgeDao
import com.jaroapps.dailybrain.core.database.dao.UserGiftDao
import com.jaroapps.dailybrain.core.database.dao.FeedEventDao
import com.jaroapps.dailybrain.core.database.dao.OnboardingProgressDao
import com.jaroapps.dailybrain.core.database.dao.UserInventoryDao
import com.jaroapps.dailybrain.core.database.dao.GiftOutboxDao
import com.jaroapps.dailybrain.core.database.dao.PendingProfileDeletionDao
import com.jaroapps.dailybrain.core.database.dao.StreakPatchDao
import com.jaroapps.dailybrain.core.database.dao.AppAnnouncementDao
import com.jaroapps.dailybrain.core.database.entity.DailyExerciseResultEntity
import com.jaroapps.dailybrain.core.database.entity.TrainingDayEntity
import com.jaroapps.dailybrain.core.database.entity.TrainingSessionEntity
import com.jaroapps.dailybrain.core.database.entity.OverallStatisticsEntity
import com.jaroapps.dailybrain.core.database.entity.ExerciseStatisticsEntity
import com.jaroapps.dailybrain.core.database.entity.UserProfileEntity
import com.jaroapps.dailybrain.core.database.entity.AppSettingsEntity
import com.jaroapps.dailybrain.core.database.entity.OnboardingProgressEntity
import com.jaroapps.dailybrain.core.database.entity.RewardLedgerEntity
import com.jaroapps.dailybrain.core.database.entity.UserBadgeEntity
import com.jaroapps.dailybrain.core.database.entity.UserGiftEntity
import com.jaroapps.dailybrain.core.database.entity.FeedEventEntity
import com.jaroapps.dailybrain.core.database.entity.UserInventoryEntity
import com.jaroapps.dailybrain.core.database.entity.GiftOutboxEntity
import com.jaroapps.dailybrain.core.database.entity.PendingProfileDeletionEntity
import com.jaroapps.dailybrain.core.database.entity.StreakPatchDayEntity
import com.jaroapps.dailybrain.core.database.entity.StreakPatchStateEntity
import com.jaroapps.dailybrain.core.database.entity.AppAnnouncementEntity

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
        RewardLedgerEntity::class,
        UserBadgeEntity::class,
        UserGiftEntity::class,
        FeedEventEntity::class,
        UserInventoryEntity::class,
        GiftOutboxEntity::class,
        PendingProfileDeletionEntity::class,
        StreakPatchDayEntity::class,
        StreakPatchStateEntity::class,
        AppAnnouncementEntity::class,
    ],
    version = 24,
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
    abstract fun rewardLedgerDao(): RewardLedgerDao
    abstract fun userBadgeDao(): UserBadgeDao
    abstract fun userGiftDao(): UserGiftDao
    abstract fun feedEventDao(): FeedEventDao
    abstract fun userInventoryDao(): UserInventoryDao
    abstract fun giftOutboxDao(): GiftOutboxDao
    abstract fun pendingProfileDeletionDao(): PendingProfileDeletionDao
    abstract fun streakPatchDao(): StreakPatchDao
    abstract fun appAnnouncementDao(): AppAnnouncementDao
}

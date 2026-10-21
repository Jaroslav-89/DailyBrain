package com.jaroapps.dailybrain.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jaroapps.dailybrain.core.database.entity.OnboardingProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OnboardingProgressDao {
    @Query("SELECT * FROM onboarding_progress WHERE localAccountId = :localAccountId")
    fun observeProgress(localAccountId: String): Flow<OnboardingProgressEntity?>

    @Query("SELECT * FROM onboarding_progress WHERE localAccountId = :localAccountId")
    suspend fun getProgress(localAccountId: String): OnboardingProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(progress: OnboardingProgressEntity)

    @Query("DELETE FROM onboarding_progress WHERE localAccountId = :localAccountId")
    suspend fun deleteProgress(localAccountId: String)
}

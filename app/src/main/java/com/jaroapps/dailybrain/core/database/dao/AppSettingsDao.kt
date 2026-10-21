package com.jaroapps.dailybrain.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jaroapps.dailybrain.core.database.entity.AppSettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppSettingsDao {
    @Query("SELECT activeLocalAccountId FROM app_settings WHERE id = 1")
    fun observeActiveLocalAccountId(): Flow<String?>

    @Query("SELECT activeLocalAccountId FROM app_settings WHERE id = 1")
    suspend fun getActiveLocalAccountId(): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setActiveAccount(settings: AppSettingsEntity)
}
package com.jaroapps.dailybrain.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jaroapps.dailybrain.core.database.entity.StreakPatchDayEntity
import com.jaroapps.dailybrain.core.database.entity.StreakPatchStateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StreakPatchDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDayIfAbsent(day: StreakPatchDayEntity): Long

    @Query("SELECT * FROM streak_patch_days WHERE userId = :userId ORDER BY date ASC")
    suspend fun getDays(userId: String): List<StreakPatchDayEntity>

    @Query("SELECT * FROM streak_patch_days WHERE userId = :userId ORDER BY date ASC")
    fun observeDays(userId: String): Flow<List<StreakPatchDayEntity>>

    @Query("SELECT * FROM streak_patch_state WHERE userId = :userId LIMIT 1")
    suspend fun getState(userId: String): StreakPatchStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveState(state: StreakPatchStateEntity)

    @Query("DELETE FROM streak_patch_days WHERE userId = :userId")
    suspend fun deleteDaysForUser(userId: String)

    @Query("DELETE FROM streak_patch_state WHERE userId = :userId")
    suspend fun deleteStateForUser(userId: String)
}

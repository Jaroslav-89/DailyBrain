package com.jaroapps.dailybrain.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jaroapps.dailybrain.core.database.entity.UserBadgeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserBadgeDao {
    @Query("SELECT * FROM user_badges WHERE ownerUserId = :ownerUserId ORDER BY awardedAtEpochMs DESC")
    fun observeForUser(ownerUserId: String): Flow<List<UserBadgeEntity>>

    @Query("SELECT * FROM user_badges WHERE ownerUserId = :ownerUserId")
    suspend fun getForUser(ownerUserId: String): List<UserBadgeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(badges: List<UserBadgeEntity>)

    @Query("DELETE FROM user_badges WHERE ownerUserId = :ownerUserId")
    suspend fun deleteForUser(ownerUserId: String)
}

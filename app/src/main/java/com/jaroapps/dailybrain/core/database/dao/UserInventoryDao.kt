package com.jaroapps.dailybrain.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jaroapps.dailybrain.core.database.entity.UserInventoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserInventoryDao {
    @Query("SELECT * FROM user_inventory WHERE ownerUserId = :ownerUserId ORDER BY acquiredAtEpochMs DESC")
    fun observeForUser(ownerUserId: String): Flow<List<UserInventoryEntity>>

    @Query("SELECT * FROM user_inventory WHERE ownerUserId = :ownerUserId")
    suspend fun getForUser(ownerUserId: String): List<UserInventoryEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM user_inventory WHERE ownerUserId = :ownerUserId AND itemId = :itemId AND itemType = :itemType)")
    suspend fun ownsItem(ownerUserId: String, itemId: String, itemType: String): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(item: UserInventoryEntity): Long

    @Query("DELETE FROM user_inventory WHERE ownerUserId = :ownerUserId")
    suspend fun deleteForUser(ownerUserId: String)
}

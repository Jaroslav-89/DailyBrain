package com.jaroapps.dailybrain.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jaroapps.dailybrain.core.database.entity.UserGiftEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserGiftDao {
    @Query("SELECT * FROM user_gifts WHERE ownerUserId = :ownerUserId ORDER BY receivedAtEpochMs DESC")
    fun observeForUser(ownerUserId: String): Flow<List<UserGiftEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(gift: UserGiftEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(gifts: List<UserGiftEntity>)

    @Query("UPDATE user_gifts SET isOpened = 1 WHERE ownerUserId = :ownerUserId AND giftId = :giftId")
    suspend fun markOpened(ownerUserId: String, giftId: String)

    @Query("SELECT * FROM user_gifts WHERE ownerUserId = :ownerUserId AND giftId = :giftId LIMIT 1")
    suspend fun getForUser(ownerUserId: String, giftId: String): UserGiftEntity?

    @Query("UPDATE user_gifts SET senderUserId = :senderUid, senderName = :senderName WHERE ownerUserId = :ownerUserId AND giftId = :giftId")
    suspend fun updateSenderInfo(ownerUserId: String, giftId: String, senderUid: String, senderName: String?)

    @Query("DELETE FROM user_gifts WHERE ownerUserId = :ownerUserId")
    suspend fun deleteForUser(ownerUserId: String)
}

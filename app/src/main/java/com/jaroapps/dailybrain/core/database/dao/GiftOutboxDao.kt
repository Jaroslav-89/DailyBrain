package com.jaroapps.dailybrain.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jaroapps.dailybrain.core.database.entity.GiftOutboxEntity

@Dao
interface GiftOutboxDao {
    @Query("SELECT * FROM gift_outbox WHERE ownerUserId = :ownerUserId AND isSent = 0 ORDER BY createdAtEpochMs ASC")
    suspend fun getPendingForUser(ownerUserId: String): List<GiftOutboxEntity>

    @Query("SELECT * FROM gift_outbox WHERE ownerUserId = :ownerUserId ORDER BY createdAtEpochMs DESC")
    fun observeForUser(ownerUserId: String): kotlinx.coroutines.flow.Flow<List<GiftOutboxEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM gift_outbox WHERE ownerUserId = :ownerUserId AND giftId = :giftId)")
    suspend fun exists(ownerUserId: String, giftId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(item: GiftOutboxEntity): Long

    @Query("UPDATE gift_outbox SET isSent = 1 WHERE ownerUserId = :ownerUserId AND giftId = :giftId")
    suspend fun markSent(ownerUserId: String, giftId: String)

    @Query("DELETE FROM gift_outbox WHERE ownerUserId = :ownerUserId")
    suspend fun deleteForUser(ownerUserId: String)
}

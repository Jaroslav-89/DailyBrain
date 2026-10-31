package com.jaroapps.dailybrain.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jaroapps.dailybrain.core.database.entity.FeedEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FeedEventDao {
    @Query("SELECT * FROM feed_events WHERE localAccountId = :localAccountId AND isRevoked = 0 ORDER BY occurredAtEpochMs DESC")
    fun observeFeed(localAccountId: String): Flow<List<FeedEventEntity>>

    @Query("SELECT * FROM feed_events WHERE localAccountId = :localAccountId AND isLocal = 1")
    suspend fun getLocalEvents(localAccountId: String): List<FeedEventEntity>

    @Query("SELECT * FROM feed_events WHERE localAccountId = :localAccountId AND isLocal = 1 AND isRevoked = 1 AND syncState = 'PENDING_DELETE'")
    suspend fun getPendingDeletes(localAccountId: String): List<FeedEventEntity>

    @Query("SELECT * FROM feed_events WHERE localAccountId = :localAccountId AND isLocal = 1 AND isRevoked = 0 AND authorFirebaseUid IS NOT NULL ORDER BY occurredAtEpochMs DESC LIMIT :limit")
    suspend fun getLatestPublicEvents(localAccountId: String, limit: Int): List<FeedEventEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(events: List<FeedEventEntity>)

    @Query("UPDATE feed_events SET syncState = 'SYNCED' WHERE localAccountId = :localAccountId AND eventId = :eventId")
    suspend fun markSynced(localAccountId: String, eventId: String)

    @Query("DELETE FROM feed_events WHERE localAccountId = :localAccountId AND isLocal = 0")
    suspend fun deleteCachedFriendEvents(localAccountId: String)

    @Query("DELETE FROM feed_events WHERE localAccountId = :localAccountId")
    suspend fun deleteForLocalAccount(localAccountId: String)
}

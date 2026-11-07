package com.jaroapps.dailybrain.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jaroapps.dailybrain.core.database.entity.FeedEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FeedEventDao {
    @Query("""
        SELECT * FROM feed_events
        WHERE localAccountId = :localAccountId AND isRevoked = 0
        ORDER BY occurredAtEpochMs DESC, eventId DESC
        LIMIT :limit
    """)
    fun observeFeed(localAccountId: String, limit: Int): Flow<List<FeedEventEntity>>

    @Query("""
        SELECT * FROM feed_events
        WHERE localAccountId = :localAccountId AND isRevoked = 0
          AND (
            occurredAtEpochMs < :beforeOccurredAtEpochMs
            OR (occurredAtEpochMs = :beforeOccurredAtEpochMs AND eventId < :beforeEventId)
          )
        ORDER BY occurredAtEpochMs DESC, eventId DESC
        LIMIT :limit
    """)
    suspend fun getOlderFeedEvents(
        localAccountId: String,
        beforeOccurredAtEpochMs: Long,
        beforeEventId: String,
        limit: Int,
    ): List<FeedEventEntity>

    @Query("SELECT * FROM feed_events WHERE localAccountId = :localAccountId AND eventId = :eventId LIMIT 1")
    suspend fun getEvent(localAccountId: String, eventId: String): FeedEventEntity?

    @Query("SELECT * FROM feed_events WHERE localAccountId = :localAccountId AND isLocal = 1")
    suspend fun getLocalEvents(localAccountId: String): List<FeedEventEntity>

    @Query("SELECT * FROM feed_events WHERE localAccountId = :localAccountId AND isLocal = 1 AND isRevoked = 1 AND syncState = 'PENDING_DELETE'")
    suspend fun getPendingDeletes(localAccountId: String): List<FeedEventEntity>

    @Query("SELECT * FROM feed_events WHERE localAccountId = :localAccountId AND isLocal = 1 AND isRevoked = 0 AND authorFirebaseUid IS NOT NULL ORDER BY occurredAtEpochMs DESC LIMIT :limit")
    suspend fun getLatestPublicEvents(localAccountId: String, limit: Int): List<FeedEventEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(events: List<FeedEventEntity>)

    @Query("UPDATE feed_events SET syncState = 'LOCAL_ONLY' WHERE localAccountId = :localAccountId AND authorFirebaseUid = :firebaseUid AND isLocal = 1 AND isRevoked = 0")
    suspend fun markOwnEventsLocalOnly(localAccountId: String, firebaseUid: String)

    @Query("UPDATE feed_events SET syncState = 'SYNCED' WHERE localAccountId = :localAccountId AND eventId = :eventId")
    suspend fun markSynced(localAccountId: String, eventId: String)

    @Query("UPDATE feed_events SET syncState = :syncState WHERE localAccountId = :localAccountId AND eventId = :eventId")
    suspend fun updateSyncState(localAccountId: String, eventId: String, syncState: String)

    @Query("DELETE FROM feed_events WHERE localAccountId = :localAccountId AND isLocal = 0 AND eventType != 'NEW_FOLLOWER'")
    suspend fun deleteCachedFriendEvents(localAccountId: String)

    @Query("DELETE FROM feed_events WHERE localAccountId = :localAccountId")
    suspend fun deleteForLocalAccount(localAccountId: String)
}

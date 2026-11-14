package com.jaroapps.dailybrain.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jaroapps.dailybrain.core.database.entity.PendingGameMoveEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingGameMoveDao {
    @Query("SELECT * FROM pending_game_moves WHERE ownerUserId = :ownerUserId ORDER BY createdAtEpochMs ASC")
    suspend fun getPendingForUser(ownerUserId: String): List<PendingGameMoveEntity>

    @Query("SELECT * FROM pending_game_moves WHERE ownerUserId = :ownerUserId AND roomId = :roomId ORDER BY createdAtEpochMs ASC")
    suspend fun getPendingForRoom(ownerUserId: String, roomId: String): List<PendingGameMoveEntity>

    @Query("SELECT * FROM pending_game_moves WHERE ownerUserId = :ownerUserId AND roomId = :roomId")
    fun observePendingForRoom(ownerUserId: String, roomId: String): Flow<List<PendingGameMoveEntity>>

    @Query("SELECT * FROM pending_game_moves WHERE ownerUserId = :ownerUserId AND roomId = :roomId AND roundId = :roundId AND opponentUid = :opponentUid LIMIT 1")
    suspend fun getPending(
        ownerUserId: String,
        roomId: String,
        roundId: String,
        opponentUid: String,
    ): PendingGameMoveEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(move: PendingGameMoveEntity): Long

    @Query("DELETE FROM pending_game_moves WHERE ownerUserId = :ownerUserId AND roomId = :roomId AND roundId = :roundId AND opponentUid = :opponentUid")
    suspend fun delete(
        ownerUserId: String,
        roomId: String,
        roundId: String,
        opponentUid: String,
    )

    @Query("DELETE FROM pending_game_moves WHERE ownerUserId = :ownerUserId AND roomId = :roomId")
    suspend fun deleteForRoom(ownerUserId: String, roomId: String)

    @Query("DELETE FROM pending_game_moves WHERE ownerUserId = :ownerUserId")
    suspend fun deleteForUser(ownerUserId: String)
}

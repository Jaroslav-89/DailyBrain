package com.jaroapps.dailybrain.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jaroapps.dailybrain.core.database.entity.PendingProfileDeletionEntity

@Dao
interface PendingProfileDeletionDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(deletion: PendingProfileDeletionEntity): Long

    @Query("SELECT * FROM pending_profile_deletions WHERE firebaseUid = :firebaseUid LIMIT 1")
    suspend fun getByFirebaseUid(firebaseUid: String): PendingProfileDeletionEntity?

    @Query("SELECT * FROM pending_profile_deletions WHERE googleSubject = :googleSubject ORDER BY requestedAtEpochMs ASC LIMIT 1")
    suspend fun getByGoogleSubject(googleSubject: String): PendingProfileDeletionEntity?

    @Query("UPDATE pending_profile_deletions SET status = :status WHERE firebaseUid = :firebaseUid")
    suspend fun updateStatus(firebaseUid: String, status: String): Int

    @Query("DELETE FROM pending_profile_deletions WHERE firebaseUid = :firebaseUid")
    suspend fun delete(firebaseUid: String): Int
}

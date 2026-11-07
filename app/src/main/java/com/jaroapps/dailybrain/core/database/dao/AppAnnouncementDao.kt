package com.jaroapps.dailybrain.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jaroapps.dailybrain.core.database.entity.AppAnnouncementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppAnnouncementDao {
    @Query("""
        SELECT * FROM app_announcements
        WHERE userId = :userId AND isDismissed = 0
        ORDER BY createdAtEpochMs ASC, announcementId ASC
        LIMIT 1
    """)
    fun observeNext(userId: String): Flow<AppAnnouncementEntity?>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(announcement: AppAnnouncementEntity): Long

    @Query("""
        UPDATE app_announcements
        SET isDismissed = 1
        WHERE userId = :userId AND announcementId = :announcementId
    """)
    suspend fun markDismissed(userId: String, announcementId: String)

    @Query("DELETE FROM app_announcements WHERE userId = :userId")
    suspend fun deleteForUser(userId: String)
}

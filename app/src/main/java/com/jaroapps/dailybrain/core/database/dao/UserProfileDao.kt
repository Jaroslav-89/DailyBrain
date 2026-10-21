package com.jaroapps.dailybrain.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jaroapps.dailybrain.core.database.entity.UserProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profile WHERE localAccountId = :localAccountId")
    fun observeProfile(localAccountId: String): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profile WHERE localAccountId = :localAccountId")
    suspend fun getProfile(localAccountId: String): UserProfileEntity?

    @Query("SELECT * FROM user_profile WHERE id = 1")
    suspend fun getLegacyDefaultProfile(): UserProfileEntity?

    @Query("SELECT avatarJson FROM user_profile WHERE localAccountId = :localAccountId")
    fun observeAvatarJson(localAccountId: String): Flow<String?>

    @Query("UPDATE user_profile SET avatarJson = :avatarJson, updatedAtEpochMs = :updatedAtEpochMs WHERE localAccountId = :localAccountId")
    suspend fun updateAvatarJson(localAccountId: String, avatarJson: String?, updatedAtEpochMs: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(profile: UserProfileEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(profile: UserProfileEntity)

    @Query("SELECT * FROM user_profile ORDER BY id ASC")
    fun observeProfiles(): Flow<List<UserProfileEntity>>

    @Query("SELECT * FROM user_profile ORDER BY id ASC")
    suspend fun getProfiles(): List<UserProfileEntity>

    @Query("DELETE FROM user_profile WHERE localAccountId = :localAccountId")
    suspend fun deleteByLocalAccountId(localAccountId: String): Int
}
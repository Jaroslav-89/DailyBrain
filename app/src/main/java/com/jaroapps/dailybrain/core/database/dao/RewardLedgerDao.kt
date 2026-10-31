package com.jaroapps.dailybrain.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jaroapps.dailybrain.core.database.entity.RewardLedgerEntity

@Dao
interface RewardLedgerDao {
    @Query("SELECT * FROM reward_ledger WHERE userId = :userId")
    suspend fun getForUser(userId: String): List<RewardLedgerEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entries: List<RewardLedgerEntity>)

    @Query("SELECT COALESCE(SUM(deltaXp), 0) FROM reward_ledger WHERE userId = :userId AND isActive = 1")
    suspend fun getXpBalance(userId: String): Int

    @Query("SELECT COALESCE(SUM(deltaEndorphins), 0) FROM reward_ledger WHERE userId = :userId AND isActive = 1")
    suspend fun getEndorphinBalance(userId: String): Int

    @Query("DELETE FROM reward_ledger WHERE userId = :userId")
    suspend fun deleteForUser(userId: String)
}

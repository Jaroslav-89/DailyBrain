package com.jaroapps.dailybrain.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.jaroapps.dailybrain.core.database.entity.TrainingSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TrainingSessionDao {
    @Insert suspend fun insert(session: TrainingSessionEntity): Long

    @Query("SELECT * FROM training_sessions WHERE userId = :userId ORDER BY finishedAtEpochMs ASC")
    fun observeAll(userId: String): Flow<List<TrainingSessionEntity>>

    @Query("SELECT COUNT(*) FROM training_sessions WHERE userId = :userId")
    fun observeTotalCount(userId: String): Flow<Int>

    @Query("SELECT MIN(startedAtEpochMs) FROM training_sessions WHERE userId = :userId")
    suspend fun getFirstTrainingStartedAtEpochMs(userId: String): Long?

    @Query("SELECT MIN(startedAtEpochMs) FROM training_sessions WHERE userId = :userId")
    fun observeFirstTrainingStartedAtEpochMs(userId: String): Flow<Long?>

    @Query("SELECT * FROM training_sessions WHERE id = :id AND userId = :userId")
    suspend fun getById(id: Long, userId: String): TrainingSessionEntity?

    @Query("DELETE FROM training_sessions WHERE id = :id AND userId = :userId")
    suspend fun deleteById(id: Long, userId: String): Int

    @Query("SELECT * FROM training_sessions WHERE userId = :userId ORDER BY finishedAtEpochMs ASC")
    suspend fun getAll(userId: String): List<TrainingSessionEntity>

    @Query("SELECT * FROM training_sessions WHERE userId = :userId AND date = :date ORDER BY finishedAtEpochMs ASC")
    fun observeByDate(userId: String, date: Int): Flow<List<TrainingSessionEntity>>

    @Query("SELECT * FROM training_sessions WHERE userId = :userId AND date = :date ORDER BY finishedAtEpochMs ASC")
    suspend fun getAllByDate(userId: String, date: Int): List<TrainingSessionEntity>
}
package com.jaroapps.dailybrain.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.jaroapps.dailybrain.core.database.entity.TrainingSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TrainingSessionDao {

    @Insert
    suspend fun insert(
        session: TrainingSessionEntity,
    ): Long

    @Query(
        """
        SELECT * FROM training_sessions
        ORDER BY finishedAtEpochMs DESC
        """
    )
    fun observeAll(): Flow<List<TrainingSessionEntity>>

    @Query("SELECT * FROM training_sessions WHERE id = :id")
    suspend fun getById(id: Long): TrainingSessionEntity?
}
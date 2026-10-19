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
        ORDER BY finishedAtEpochMs ASC
        """
    )
    fun observeAll(): Flow<List<TrainingSessionEntity>>

    @Query("SELECT COUNT(*) FROM training_sessions")
    fun observeTotalCount(): Flow<Int>

    @Query("SELECT * FROM training_sessions WHERE id = :id")
    suspend fun getById(id: Long): TrainingSessionEntity?

    @Query(
        """
        SELECT * FROM training_sessions
        WHERE date = :date
        ORDER BY finishedAtEpochMs ASC
        """
    )
    fun observeByDate(date: Int): Flow<List<TrainingSessionEntity>>
}

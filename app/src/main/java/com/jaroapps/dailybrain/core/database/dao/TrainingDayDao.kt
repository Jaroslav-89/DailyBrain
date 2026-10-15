package com.jaroapps.dailybrain.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jaroapps.dailybrain.core.database.entity.TrainingDayEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TrainingDayDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(day: TrainingDayEntity): Long

    @Query(
        """
        UPDATE training_days
        SET exerciseCount = exerciseCount + 1
        WHERE date = :date
        """
    )
    suspend fun incrementExerciseCount(date: Int)

    @Query("SELECT * FROM training_days ORDER BY date ASC")
    fun observeAll(): Flow<List<TrainingDayEntity>>
}
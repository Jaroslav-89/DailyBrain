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
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(days: List<TrainingDayEntity>)
    @Query("DELETE FROM training_days WHERE userId = :userId")
    suspend fun deleteAll(userId: String)
    @Query("UPDATE training_days SET exerciseCount = exerciseCount + 1 WHERE userId = :userId AND date = :date")
    suspend fun incrementExerciseCount(userId: String, date: Int)
    @Query("SELECT EXISTS(SELECT 1 FROM training_days WHERE userId = :userId AND date = :date)")
    suspend fun hasTrainingOnDate(userId: String, date: Int): Boolean
    @Query("SELECT * FROM training_days WHERE userId = :userId ORDER BY date ASC")
    fun observeAll(userId: String): Flow<List<TrainingDayEntity>>
    @Query("SELECT * FROM training_days WHERE userId = :userId ORDER BY date ASC")
    suspend fun getAll(userId: String): List<TrainingDayEntity>
    @Query("SELECT * FROM training_days WHERE userId = :userId AND date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    fun observeBetweenDates(userId: String, startDate: Int, endDate: Int): Flow<List<TrainingDayEntity>>
}

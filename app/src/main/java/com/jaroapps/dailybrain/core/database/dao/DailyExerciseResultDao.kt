package com.jaroapps.dailybrain.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jaroapps.dailybrain.core.database.entity.DailyExerciseResultEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyExerciseResultDao {
    @Query("SELECT * FROM daily_exercise_results ORDER BY date ASC")
    fun observeAll(): Flow<List<DailyExerciseResultEntity>>

    @Query("SELECT * FROM daily_exercise_results WHERE date = :date")
    suspend fun getByDate(date: Int): DailyExerciseResultEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(result: DailyExerciseResultEntity)

    @Query("DELETE FROM daily_exercise_results WHERE date = :date")
    suspend fun deleteByDate(date: Int)
}

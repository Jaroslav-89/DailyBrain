package com.jaroapps.dailybrain.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jaroapps.dailybrain.core.database.entity.ExerciseStatisticsEntity
import com.jaroapps.dailybrain.core.database.entity.OverallStatisticsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StatisticsDao {
    @Query("SELECT * FROM overall_statistics WHERE userId = :userId")
    suspend fun getOverall(userId: String): OverallStatisticsEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveOverall(statistics: OverallStatisticsEntity)
    @Query("DELETE FROM overall_statistics WHERE userId = :userId")
    suspend fun deleteOverall(userId: String)
    @Query("SELECT * FROM overall_statistics WHERE userId = :userId")
    fun observeOverall(userId: String): Flow<OverallStatisticsEntity?>
    @Query("SELECT * FROM exercise_statistics WHERE userId = :userId ORDER BY exerciseType ASC")
    fun observeExerciseStatistics(userId: String): Flow<List<ExerciseStatisticsEntity>>
    @Query("SELECT * FROM exercise_statistics WHERE userId = :userId AND exerciseType = :exerciseType")
    suspend fun getExerciseStatistics(userId: String, exerciseType: String): ExerciseStatisticsEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveExerciseStatistics(statistics: ExerciseStatisticsEntity)
    @Query("DELETE FROM exercise_statistics WHERE userId = :userId")
    suspend fun deleteAllExerciseStatistics(userId: String)
}
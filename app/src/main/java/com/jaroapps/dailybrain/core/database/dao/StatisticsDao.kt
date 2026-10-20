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

    @Query("SELECT * FROM overall_statistics WHERE id = 1")
    suspend fun getOverall(): OverallStatisticsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveOverall(statistics: OverallStatisticsEntity)

    @Query("DELETE FROM overall_statistics WHERE id = 1")
    suspend fun deleteOverall()

    @Query("SELECT * FROM overall_statistics WHERE id = 1")
    fun observeOverall(): Flow<OverallStatisticsEntity?>

    @Query("SELECT * FROM exercise_statistics ORDER BY exerciseType ASC")
    fun observeExerciseStatistics(): Flow<List<ExerciseStatisticsEntity>>

    @Query("SELECT * FROM exercise_statistics WHERE exerciseType = :exerciseType")
    suspend fun getExerciseStatistics(exerciseType: String): ExerciseStatisticsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveExerciseStatistics(statistics: ExerciseStatisticsEntity)

    @Query("DELETE FROM exercise_statistics")
    suspend fun deleteAllExerciseStatistics()
}

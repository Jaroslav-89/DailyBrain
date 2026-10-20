package com.jaroapps.dailybrain.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.jaroapps.dailybrain.core.database.entity.DailyExerciseResultEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyExerciseResultDao {

    @Query("SELECT * FROM daily_exercise_results ORDER BY date ASC")
    fun observeAll(): Flow<List<DailyExerciseResultEntity>>

    @Query("SELECT * FROM daily_exercise_results WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    fun observeBetween(startDate: Int, endDate: Int): Flow<List<DailyExerciseResultEntity>>

    @Query("SELECT * FROM daily_exercise_results WHERE date = :date")
    suspend fun getByDate(date: Int): DailyExerciseResultEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(result: DailyExerciseResultEntity)

    @Transaction
    suspend fun recordArithmeticBest(date: Int, durationMs: Long) {
        val current = getByDate(date)
        if (current == null) {
            upsert(DailyExerciseResultEntity(date, durationMs, null, null, null))
        } else if (current.arithmeticBestTimeMs == null || durationMs < current.arithmeticBestTimeMs) {
            upsert(current.copy(arithmeticBestTimeMs = durationMs))
        }
    }

    @Transaction
    suspend fun recordMemoryBest(date: Int, correctCount: Int) {
        val current = getByDate(date)
        if (current == null) {
            upsert(DailyExerciseResultEntity(date, null, correctCount, null, null))
        } else if (current.memoryBestCorrectCount == null || correctCount > current.memoryBestCorrectCount) {
            upsert(current.copy(memoryBestCorrectCount = correctCount))
        }
    }

    @Transaction
    suspend fun recordStroopBest(date: Int, durationMs: Long) {
        val current = getByDate(date)
        if (current == null) {
            upsert(DailyExerciseResultEntity(date, null, null, durationMs, null))
        } else if (current.stroopBestTimeMs == null || durationMs < current.stroopBestTimeMs) {
            upsert(current.copy(stroopBestTimeMs = durationMs))
        }
    }

    @Transaction
    suspend fun recordCountingBest(date: Int, durationMs: Long) {
        val current = getByDate(date)
        if (current == null) {
            upsert(DailyExerciseResultEntity(date, null, null, null, durationMs))
        } else if (current.countingBestTimeMs == null || durationMs < current.countingBestTimeMs) {
            upsert(current.copy(countingBestTimeMs = durationMs))
        }
    }

    @Query("DELETE FROM daily_exercise_results WHERE date = :date")
    suspend fun deleteByDate(date: Int)
}

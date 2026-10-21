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
    @Query("SELECT * FROM daily_exercise_results WHERE userId = :userId ORDER BY date ASC")
    fun observeAll(userId: String): Flow<List<DailyExerciseResultEntity>>
    @Query("SELECT * FROM daily_exercise_results WHERE userId = :userId AND date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    fun observeBetween(userId: String, startDate: Int, endDate: Int): Flow<List<DailyExerciseResultEntity>>
    @Query("SELECT * FROM daily_exercise_results WHERE userId = :userId AND date = :date")
    suspend fun getByDate(userId: String, date: Int): DailyExerciseResultEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(result: DailyExerciseResultEntity)

    @Transaction
    suspend fun recordArithmeticBest(userId: String, date: Int, durationMs: Long) {
        val current = getByDate(userId, date)
        if (current == null) upsert(DailyExerciseResultEntity(userId, date, durationMs, null, null, null))
        else if (current.arithmeticBestTimeMs == null || durationMs < current.arithmeticBestTimeMs) upsert(current.copy(arithmeticBestTimeMs = durationMs))
    }

    @Transaction
    suspend fun recordMemoryBest(userId: String, date: Int, correctCount: Int) {
        val current = getByDate(userId, date)
        if (current == null) upsert(DailyExerciseResultEntity(userId, date, null, correctCount, null, null))
        else if (current.memoryBestCorrectCount == null || correctCount > current.memoryBestCorrectCount) upsert(current.copy(memoryBestCorrectCount = correctCount))
    }

    @Transaction
    suspend fun recordStroopBest(userId: String, date: Int, durationMs: Long) {
        val current = getByDate(userId, date)
        if (current == null) upsert(DailyExerciseResultEntity(userId, date, null, null, durationMs, null))
        else if (current.stroopBestTimeMs == null || durationMs < current.stroopBestTimeMs) upsert(current.copy(stroopBestTimeMs = durationMs))
    }

    @Transaction
    suspend fun recordCountingBest(userId: String, date: Int, durationMs: Long) {
        val current = getByDate(userId, date)
        if (current == null) upsert(DailyExerciseResultEntity(userId, date, null, null, null, durationMs))
        else if (current.countingBestTimeMs == null || durationMs < current.countingBestTimeMs) upsert(current.copy(countingBestTimeMs = durationMs))
    }

    @Query("DELETE FROM daily_exercise_results WHERE userId = :userId AND date = :date")
    suspend fun deleteByDate(userId: String, date: Int)
}
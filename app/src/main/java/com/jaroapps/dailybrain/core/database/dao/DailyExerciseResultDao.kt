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

    @Query("SELECT * FROM daily_exercise_results WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    fun observeBetween(startDate: Int, endDate: Int): Flow<List<DailyExerciseResultEntity>>

    @Query("SELECT * FROM daily_exercise_results WHERE date = :date")
    suspend fun getByDate(date: Int): DailyExerciseResultEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(result: DailyExerciseResultEntity)

    @Query("""
        INSERT INTO daily_exercise_results(date, arithmeticBestTimeMs, memoryBestCorrectCount, stroopBestTimeMs, countingBestTimeMs)
        VALUES(:date, :durationMs, NULL, NULL, NULL)
        ON CONFLICT(date) DO UPDATE SET
            arithmeticBestTimeMs = CASE
                WHEN excluded.arithmeticBestTimeMs IS NOT NULL AND
                    (daily_exercise_results.arithmeticBestTimeMs IS NULL OR excluded.arithmeticBestTimeMs < daily_exercise_results.arithmeticBestTimeMs)
                THEN excluded.arithmeticBestTimeMs ELSE daily_exercise_results.arithmeticBestTimeMs END
    """)
    suspend fun recordArithmeticBest(date: Int, durationMs: Long)

    @Query("""
        INSERT INTO daily_exercise_results(date, arithmeticBestTimeMs, memoryBestCorrectCount, stroopBestTimeMs, countingBestTimeMs)
        VALUES(:date, NULL, :correctCount, NULL, NULL)
        ON CONFLICT(date) DO UPDATE SET
            memoryBestCorrectCount = CASE
                WHEN excluded.memoryBestCorrectCount IS NOT NULL AND
                    (daily_exercise_results.memoryBestCorrectCount IS NULL OR excluded.memoryBestCorrectCount > daily_exercise_results.memoryBestCorrectCount)
                THEN excluded.memoryBestCorrectCount ELSE daily_exercise_results.memoryBestCorrectCount END
    """)
    suspend fun recordMemoryBest(date: Int, correctCount: Int)

    @Query("""
        INSERT INTO daily_exercise_results(date, arithmeticBestTimeMs, memoryBestCorrectCount, stroopBestTimeMs, countingBestTimeMs)
        VALUES(:date, NULL, NULL, :durationMs, NULL)
        ON CONFLICT(date) DO UPDATE SET
            stroopBestTimeMs = CASE
                WHEN excluded.stroopBestTimeMs IS NOT NULL AND
                    (daily_exercise_results.stroopBestTimeMs IS NULL OR excluded.stroopBestTimeMs < daily_exercise_results.stroopBestTimeMs)
                THEN excluded.stroopBestTimeMs ELSE daily_exercise_results.stroopBestTimeMs END
    """)
    suspend fun recordStroopBest(date: Int, durationMs: Long)

    @Query("""
        INSERT INTO daily_exercise_results(date, arithmeticBestTimeMs, memoryBestCorrectCount, stroopBestTimeMs, countingBestTimeMs)
        VALUES(:date, NULL, NULL, NULL, :durationMs)
        ON CONFLICT(date) DO UPDATE SET
            countingBestTimeMs = CASE
                WHEN excluded.countingBestTimeMs IS NOT NULL AND
                    (daily_exercise_results.countingBestTimeMs IS NULL OR excluded.countingBestTimeMs < daily_exercise_results.countingBestTimeMs)
                THEN excluded.countingBestTimeMs ELSE daily_exercise_results.countingBestTimeMs END
    """)
    suspend fun recordCountingBest(date: Int, durationMs: Long)

    @Query("DELETE FROM daily_exercise_results WHERE date = :date")
    suspend fun deleteByDate(date: Int)
}

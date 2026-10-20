package com.jaroapps.dailybrain.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Database schema migrations are kept together here. */
object DatabaseMigrations {
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                """
                CREATE TABLE training_sessions_new (
                    id INTEGER NOT NULL PRIMARY KEY,
                    exerciseType TEXT NOT NULL,
                    startedAtEpochMs INTEGER NOT NULL,
                    finishedAtEpochMs INTEGER NOT NULL,
                    date INTEGER NOT NULL,
                    durationMs INTEGER NOT NULL,
                    accuracyPercent REAL NOT NULL,
                    taskCount INTEGER NOT NULL,
                    correctCount INTEGER NOT NULL
                )
                """.trimIndent(),
            )
            database.execSQL(
                """
                INSERT INTO training_sessions_new (
                    id, exerciseType, startedAtEpochMs, finishedAtEpochMs, date,
                    durationMs, accuracyPercent, taskCount, correctCount
                )
                SELECT
                    id, exerciseType, startedAtEpochMs, finishedAtEpochMs, date,
                    durationMs, accuracyPercent, taskCount, correctCount
                FROM training_sessions
                """.trimIndent(),
            )
            database.execSQL("DROP TABLE training_sessions")
            database.execSQL("ALTER TABLE training_sessions_new RENAME TO training_sessions")
        }
    }

    /** Reconstruct cached statistics from existing history without deleting sessions. */
    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                """
                CREATE TABLE overall_statistics (
                    id INTEGER NOT NULL PRIMARY KEY,
                    totalTrainingCount INTEGER NOT NULL,
                    totalDurationMs INTEGER NOT NULL,
                    currentStreakDays INTEGER NOT NULL,
                    bestStreakDays INTEGER NOT NULL,
                    lastTrainingDate INTEGER,
                    updatedAtEpochMs INTEGER NOT NULL
                )
                """.trimIndent(),
            )
            database.execSQL(
                """
                CREATE TABLE exercise_statistics (
                    exerciseType TEXT NOT NULL PRIMARY KEY,
                    trainingCount INTEGER NOT NULL,
                    totalDurationMs INTEGER NOT NULL,
                    bestDurationMs INTEGER,
                    bestAccuracyPercent REAL,
                    bestCorrectCount INTEGER,
                    lastTrainingAtEpochMs INTEGER NOT NULL
                )
                """.trimIndent(),
            )

            val dates = mutableListOf<Int>()
            database.query("SELECT date FROM training_days ORDER BY date ASC").use { cursor ->
                while (cursor.moveToNext()) dates += cursor.getInt(0)
            }

            var currentStreak = 0
            var bestStreak = 0
            var previousDate: LocalDate? = null
            for (storedDate in dates) {
                val date = LocalDate.parse(storedDate.toString(), DateTimeFormatter.BASIC_ISO_DATE)
                currentStreak = if (previousDate?.plusDays(1) == date) currentStreak + 1 else 1
                bestStreak = maxOf(bestStreak, currentStreak)
                previousDate = date
            }

            val lastTrainingDateSql = dates.lastOrNull()?.toString() ?: "NULL"
            database.execSQL(
                """
                INSERT INTO overall_statistics (
                    id, totalTrainingCount, totalDurationMs, currentStreakDays,
                    bestStreakDays, lastTrainingDate, updatedAtEpochMs
                )
                SELECT 1, COUNT(*), COALESCE(SUM(durationMs), 0),
                    $currentStreak, $bestStreak, $lastTrainingDateSql, ${System.currentTimeMillis()}
                FROM training_sessions
                """.trimIndent(),
            )
            database.execSQL(
                """
                INSERT INTO exercise_statistics (
                    exerciseType, trainingCount, totalDurationMs, bestDurationMs,
                    bestAccuracyPercent, bestCorrectCount, lastTrainingAtEpochMs
                )
                SELECT
                    exerciseType,
                    COUNT(*),
                    COALESCE(SUM(durationMs), 0),
                    CASE
                        WHEN exerciseType = 'ARITHMETIC'
                            THEN MIN(CASE WHEN accuracyPercent >= ${StatisticsRules.ARITHMETIC_BEST_TIME_MIN_ACCURACY_PERCENT} THEN durationMs END)
                        ELSE MIN(durationMs)
                    END,
                    CASE
                        WHEN exerciseType IN ('ARITHMETIC', 'MEMORY_WORDS') AND MAX(taskCount) > 0
                            THEN MAX(accuracyPercent)
                        ELSE NULL
                    END,
                    CASE WHEN exerciseType = 'MEMORY_WORDS' THEN MAX(correctCount) ELSE NULL END,
                    MAX(finishedAtEpochMs)
                FROM training_sessions
                GROUP BY exerciseType
                """.trimIndent(),
            )
        }
    }

    /** Create daily graph records and backfill them from the full training history. */
    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                """
                CREATE TABLE daily_exercise_results (
                    date INTEGER NOT NULL PRIMARY KEY,
                    arithmeticBestTimeMs INTEGER,
                    memoryBestCorrectCount INTEGER,
                    stroopBestTimeMs INTEGER,
                    countingBestTimeMs INTEGER
                )
                """.trimIndent(),
            )
            database.execSQL(
                """
                INSERT INTO daily_exercise_results (
                    date, arithmeticBestTimeMs, memoryBestCorrectCount,
                    stroopBestTimeMs, countingBestTimeMs
                )
                SELECT
                    date,
                    MIN(CASE WHEN exerciseType = 'ARITHMETIC'
                        AND accuracyPercent >= ${StatisticsRules.ARITHMETIC_BEST_TIME_MIN_ACCURACY_PERCENT}
                        THEN durationMs END),
                    MAX(CASE WHEN exerciseType = 'MEMORY_WORDS' THEN correctCount END),
                    MIN(CASE WHEN exerciseType = 'STROOP_TEST' THEN durationMs END),
                    MIN(CASE WHEN exerciseType = 'COUNTING_TRAINING' THEN durationMs END)
                FROM training_sessions
                GROUP BY date
                """.trimIndent(),
            )
        }
    }

    /** Add the local account/profile table without touching existing training data. */
    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                """
                CREATE TABLE user_profile (
                    id INTEGER NOT NULL PRIMARY KEY,
                    localAccountId TEXT NOT NULL,
                    firebaseUid TEXT,
                    displayName TEXT,
                    generatedAlias TEXT NOT NULL,
                    gender TEXT,
                    birthDate TEXT,
                    country TEXT,
                    city TEXT,
                    createdAtEpochMs INTEGER NOT NULL,
                    updatedAtEpochMs INTEGER NOT NULL
                )
                """.trimIndent(),
            )
        }
    }

    /** Backfill existing profile registration dates from the first recorded training. */
    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                """
                UPDATE user_profile
                SET createdAtEpochMs = (
                    SELECT MIN(startedAtEpochMs) FROM training_sessions
                )
                WHERE id = 1
                    AND EXISTS (SELECT 1 FROM training_sessions)
                """.trimIndent(),
            )
        }
    }

    /** Store avatar configuration without changing existing profiles. */
    val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE user_profile ADD COLUMN avatarJson TEXT")
        }
    }
}

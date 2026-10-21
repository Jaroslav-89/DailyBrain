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

    /** Add per-account ownership while preserving all records from the existing local profile. */
    val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(database: SupportSQLiteDatabase) {
            val now = System.currentTimeMillis()
            database.execSQL(
                """
                CREATE TABLE user_profile_new (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    localAccountId TEXT NOT NULL,
                    firebaseUid TEXT,
                    displayName TEXT,
                    generatedAlias TEXT NOT NULL,
                    gender TEXT,
                    birthDate TEXT,
                    country TEXT,
                    city TEXT,
                    createdAtEpochMs INTEGER NOT NULL,
                    updatedAtEpochMs INTEGER NOT NULL,
                    avatarJson TEXT
                )
                """.trimIndent(),
            )
            database.execSQL(
                """
                INSERT INTO user_profile_new (
                    id, localAccountId, firebaseUid, displayName, generatedAlias, gender,
                    birthDate, country, city, createdAtEpochMs, updatedAtEpochMs, avatarJson
                )
                SELECT id, localAccountId, firebaseUid, displayName, generatedAlias, gender,
                    birthDate, country, city, createdAtEpochMs, updatedAtEpochMs, avatarJson
                FROM user_profile
                """.trimIndent(),
            )
            database.execSQL(
                """
                INSERT INTO user_profile_new (
                    id, localAccountId, generatedAlias, createdAtEpochMs, updatedAtEpochMs
                )
                SELECT 1, lower(hex(randomblob(16))), 'brain#100', $now, $now
                WHERE NOT EXISTS (SELECT 1 FROM user_profile_new)
                """.trimIndent(),
            )
            database.execSQL("DROP TABLE user_profile")
            database.execSQL("ALTER TABLE user_profile_new RENAME TO user_profile")
            database.execSQL("CREATE UNIQUE INDEX index_user_profile_localAccountId ON user_profile(localAccountId)")

            database.execSQL(
                """
                CREATE TABLE app_settings (
                    id INTEGER NOT NULL PRIMARY KEY,
                    activeLocalAccountId TEXT NOT NULL
                )
                """.trimIndent(),
            )
            database.execSQL(
                "INSERT INTO app_settings (id, activeLocalAccountId) SELECT 1, localAccountId FROM user_profile WHERE id = 1",
            )

            database.execSQL(
                """
                CREATE TABLE training_sessions_new (
                    id INTEGER NOT NULL PRIMARY KEY,
                    userId TEXT NOT NULL,
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
                INSERT INTO training_sessions_new
                SELECT id, (SELECT localAccountId FROM user_profile WHERE id = 1),
                    exerciseType, startedAtEpochMs, finishedAtEpochMs, date, durationMs,
                    accuracyPercent, taskCount, correctCount
                FROM training_sessions
                """.trimIndent(),
            )
            database.execSQL("DROP TABLE training_sessions")
            database.execSQL("ALTER TABLE training_sessions_new RENAME TO training_sessions")
            database.execSQL("CREATE INDEX index_training_sessions_userId_finishedAtEpochMs ON training_sessions(userId, finishedAtEpochMs)")
            database.execSQL("CREATE INDEX index_training_sessions_userId_date ON training_sessions(userId, date)")

            database.execSQL(
                """
                CREATE TABLE training_days_new (
                    userId TEXT NOT NULL,
                    date INTEGER NOT NULL,
                    exerciseCount INTEGER NOT NULL,
                    PRIMARY KEY(userId, date)
                )
                """.trimIndent(),
            )
            database.execSQL(
                """
                INSERT INTO training_days_new
                SELECT (SELECT localAccountId FROM user_profile WHERE id = 1), date, exerciseCount
                FROM training_days
                """.trimIndent(),
            )
            database.execSQL("DROP TABLE training_days")
            database.execSQL("ALTER TABLE training_days_new RENAME TO training_days")
            database.execSQL("CREATE INDEX index_training_days_userId_date ON training_days(userId, date)")

            database.execSQL(
                """
                CREATE TABLE overall_statistics_new (
                    userId TEXT NOT NULL PRIMARY KEY,
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
                INSERT INTO overall_statistics_new
                SELECT (SELECT localAccountId FROM user_profile WHERE id = 1),
                    totalTrainingCount, totalDurationMs, currentStreakDays, bestStreakDays,
                    lastTrainingDate, updatedAtEpochMs
                FROM overall_statistics WHERE id = 1
                """.trimIndent(),
            )
            database.execSQL("DROP TABLE overall_statistics")
            database.execSQL("ALTER TABLE overall_statistics_new RENAME TO overall_statistics")

            database.execSQL(
                """
                CREATE TABLE exercise_statistics_new (
                    userId TEXT NOT NULL,
                    exerciseType TEXT NOT NULL,
                    trainingCount INTEGER NOT NULL,
                    totalDurationMs INTEGER NOT NULL,
                    bestDurationMs INTEGER,
                    bestAccuracyPercent REAL,
                    bestCorrectCount INTEGER,
                    lastTrainingAtEpochMs INTEGER NOT NULL,
                    PRIMARY KEY(userId, exerciseType)
                )
                """.trimIndent(),
            )
            database.execSQL(
                """
                INSERT INTO exercise_statistics_new
                SELECT (SELECT localAccountId FROM user_profile WHERE id = 1),
                    exerciseType, trainingCount, totalDurationMs, bestDurationMs,
                    bestAccuracyPercent, bestCorrectCount, lastTrainingAtEpochMs
                FROM exercise_statistics
                """.trimIndent(),
            )
            database.execSQL("DROP TABLE exercise_statistics")
            database.execSQL("ALTER TABLE exercise_statistics_new RENAME TO exercise_statistics")
            database.execSQL("CREATE INDEX index_exercise_statistics_userId_exerciseType ON exercise_statistics(userId, exerciseType)")

            database.execSQL(
                """
                CREATE TABLE daily_exercise_results_new (
                    userId TEXT NOT NULL,
                    date INTEGER NOT NULL,
                    arithmeticBestTimeMs INTEGER,
                    memoryBestCorrectCount INTEGER,
                    stroopBestTimeMs INTEGER,
                    countingBestTimeMs INTEGER,
                    PRIMARY KEY(userId, date)
                )
                """.trimIndent(),
            )
            database.execSQL(
                """
                INSERT INTO daily_exercise_results_new
                SELECT (SELECT localAccountId FROM user_profile WHERE id = 1),
                    date, arithmeticBestTimeMs, memoryBestCorrectCount, stroopBestTimeMs, countingBestTimeMs
                FROM daily_exercise_results
                """.trimIndent(),
            )
            database.execSQL("DROP TABLE daily_exercise_results")
            database.execSQL("ALTER TABLE daily_exercise_results_new RENAME TO daily_exercise_results")
            database.execSQL("CREATE INDEX index_daily_exercise_results_userId_date ON daily_exercise_results(userId, date)")
        }
    }

    /** Store onboarding completion independently for each local profile. */
    val MIGRATION_8_9 = object : Migration(8, 9) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                """
                CREATE TABLE onboarding_progress (
                    localAccountId TEXT NOT NULL PRIMARY KEY,
                    hasSeenAppIntro INTEGER NOT NULL,
                    hasSeenArithmeticIntro INTEGER NOT NULL,
                    hasSeenMemoryIntro INTEGER NOT NULL,
                    hasSeenCountingIntro INTEGER NOT NULL,
                    hasSeenStroopIntro INTEGER NOT NULL
                )
                """.trimIndent(),
            )
        }
    }
}

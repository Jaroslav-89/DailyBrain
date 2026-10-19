package com.jaroapps.dailybrain.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Database schema migrations are kept together here.
 */
object DatabaseMigrations {

    /**
     * Version 1 used an auto-generated training session ID.
     * Version 2 uses the session start timestamp as the ID instead.
     *
     * Rebuild the table to remove SQLite's AUTOINCREMENT declaration while
     * preserving existing training records and their IDs.
     */
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
                    id,
                    exerciseType,
                    startedAtEpochMs,
                    finishedAtEpochMs,
                    date,
                    durationMs,
                    accuracyPercent,
                    taskCount,
                    correctCount
                )
                SELECT
                    id,
                    exerciseType,
                    startedAtEpochMs,
                    finishedAtEpochMs,
                    date,
                    durationMs,
                    accuracyPercent,
                    taskCount,
                    correctCount
                FROM training_sessions
                """.trimIndent(),
            )

            database.execSQL("DROP TABLE training_sessions")
            database.execSQL(
                "ALTER TABLE training_sessions_new RENAME TO training_sessions",
            )
        }
    }
}

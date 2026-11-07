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

    /** Add a per-device unique Google account binding to each local profile. */
    val MIGRATION_9_10 = object : Migration(9, 10) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE user_profile ADD COLUMN googleSubject TEXT")
            database.execSQL("ALTER TABLE user_profile ADD COLUMN googleEmail TEXT")
            database.execSQL("ALTER TABLE user_profile ADD COLUMN googleDisplayName TEXT")
            database.execSQL("ALTER TABLE user_profile ADD COLUMN googlePhotoUrl TEXT")
            database.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS index_user_profile_googleSubject ON user_profile(googleSubject)",
            )
        }
    }


    /** Persist the state where the app is waiting for the user to select a local profile. */
    val MIGRATION_10_11 = object : Migration(10, 11) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                "ALTER TABLE app_settings ADD COLUMN profileSelectionRequired INTEGER NOT NULL DEFAULT 0",
            )
        }
    }

    /** Cache the globally owned username on each local profile linked to a Google identity. */
    val MIGRATION_11_12 = object : Migration(11, 12) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE user_profile ADD COLUMN username TEXT")
        }
    }

    /** Cache the stable Firebase invite code on the matching linked local profile. */
    val MIGRATION_12_13 = object : Migration(12, 13) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE user_profile ADD COLUMN inviteCode TEXT")
        }
    }

    /** Add local rewards, badge inventory, gifts, and cached feed events. */
    val MIGRATION_13_14 = object : Migration(13, 14) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE user_profile ADD COLUMN xpPoints INTEGER NOT NULL DEFAULT 0")
            database.execSQL("ALTER TABLE user_profile ADD COLUMN endorphinBalance INTEGER NOT NULL DEFAULT 0")
            database.execSQL("ALTER TABLE user_profile ADD COLUMN patchCount INTEGER NOT NULL DEFAULT 0")
            database.execSQL("ALTER TABLE user_profile ADD COLUMN rewardsInitialized INTEGER NOT NULL DEFAULT 0")

            database.execSQL(
                """
                CREATE TABLE reward_ledger (
                    userId TEXT NOT NULL,
                    rewardKey TEXT NOT NULL,
                    deltaXp INTEGER NOT NULL,
                    deltaEndorphins INTEGER NOT NULL,
                    category TEXT NOT NULL,
                    reason TEXT NOT NULL,
                    sourceId INTEGER,
                    periodKey TEXT,
                    occurredAtEpochMs INTEGER NOT NULL,
                    isActive INTEGER NOT NULL,
                    PRIMARY KEY(userId, rewardKey)
                )
                """.trimIndent(),
            )
            database.execSQL("CREATE INDEX index_reward_ledger_userId_category ON reward_ledger(userId, category)")
            database.execSQL("CREATE INDEX index_reward_ledger_userId_periodKey ON reward_ledger(userId, periodKey)")

            database.execSQL(
                """
                CREATE TABLE user_badges (
                    ownerUserId TEXT NOT NULL,
                    badgeId TEXT NOT NULL,
                    badgeType TEXT NOT NULL,
                    periodKey TEXT NOT NULL,
                    awardedAtEpochMs INTEGER NOT NULL,
                    triggerWorkoutId INTEGER,
                    isRevoked INTEGER NOT NULL,
                    PRIMARY KEY(ownerUserId, badgeId)
                )
                """.trimIndent(),
            )
            database.execSQL("CREATE INDEX index_user_badges_ownerUserId_badgeType ON user_badges(ownerUserId, badgeType)")
            database.execSQL("CREATE INDEX index_user_badges_ownerUserId_periodKey ON user_badges(ownerUserId, periodKey)")

            database.execSQL(
                """
                CREATE TABLE user_gifts (
                    ownerUserId TEXT NOT NULL,
                    giftId TEXT NOT NULL,
                    giftType TEXT NOT NULL,
                    senderUserId TEXT,
                    receivedAtEpochMs INTEGER NOT NULL,
                    isOpened INTEGER NOT NULL,
                    PRIMARY KEY(ownerUserId, giftId)
                )
                """.trimIndent(),
            )
            database.execSQL("CREATE INDEX index_user_gifts_ownerUserId_receivedAtEpochMs ON user_gifts(ownerUserId, receivedAtEpochMs)")

            database.execSQL(
                """
                CREATE TABLE feed_events (
                    localAccountId TEXT NOT NULL,
                    eventId TEXT NOT NULL,
                    authorFirebaseUid TEXT,
                    authorName TEXT NOT NULL,
                    authorUsername TEXT,
                    eventType TEXT NOT NULL,
                    occurredAtEpochMs INTEGER NOT NULL,
                    periodKey TEXT,
                    exerciseType TEXT,
                    valueLong INTEGER,
                    valueInt INTEGER,
                    accuracyPercent REAL,
                    isLocal INTEGER NOT NULL,
                    isRevoked INTEGER NOT NULL,
                    syncState TEXT NOT NULL,
                    PRIMARY KEY(localAccountId, eventId)
                )
                """.trimIndent(),
            )
            database.execSQL("CREATE INDEX index_feed_events_localAccountId_occurredAtEpochMs ON feed_events(localAccountId, occurredAtEpochMs)")
            database.execSQL("CREATE INDEX index_feed_events_authorFirebaseUid ON feed_events(authorFirebaseUid)")
        }
    }

    /** Recalculate endorphins for profiles initialized while endorphin rewards were disabled. */
    val MIGRATION_14_15 = object : Migration(14, 15) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("UPDATE user_profile SET rewardsInitialized = 0")
        }
    }

    /** Re-evaluate weekly reward eligibility after the corrected weekly achievement rules. */
    val MIGRATION_15_16 = object : Migration(15, 16) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("UPDATE user_profile SET rewardsInitialized = 0")
        }
    }

    /** Recalculate day streaks and weekly challenge rewards using the corrected calendar rules. */
    val MIGRATION_16_17 = object : Migration(16, 17) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("UPDATE user_profile SET rewardsInitialized = 0")
        }
    }

    /** Recalculate reward eligibility using completion timestamps in the device's local time zone. */
    val MIGRATION_17_18 = object : Migration(17, 18) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("UPDATE user_profile SET rewardsInitialized = 0")
        }
    }

    /** Rebuild the complete earned-reward ledger for every profile from all saved sessions. */
    val MIGRATION_18_19 = object : Migration(18, 19) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("UPDATE user_profile SET rewardsInitialized = 0")
        }
    }
    /** Add local cosmetic inventory and a retry-safe outgoing gift queue. */
    val MIGRATION_19_20 = object : Migration(19, 20) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                """
                CREATE TABLE user_inventory (
                    ownerUserId TEXT NOT NULL,
                    itemId TEXT NOT NULL,
                    itemType TEXT NOT NULL,
                    acquiredAtEpochMs INTEGER NOT NULL,
                    PRIMARY KEY(ownerUserId, itemId)
                )
                """.trimIndent(),
            )
            database.execSQL(
                "CREATE INDEX index_user_inventory_ownerUserId_itemType ON user_inventory(ownerUserId, itemType)",
            )
            database.execSQL(
                """
                CREATE TABLE gift_outbox (
                    ownerUserId TEXT NOT NULL,
                    giftId TEXT NOT NULL,
                    senderFirebaseUid TEXT NOT NULL,
                    recipientFirebaseUid TEXT NOT NULL,
                    senderName TEXT NOT NULL,
                    giftType TEXT NOT NULL,
                    amount INTEGER NOT NULL,
                    createdAtEpochMs INTEGER NOT NULL,
                    isSent INTEGER NOT NULL,
                    PRIMARY KEY(ownerUserId, giftId)
                )
                """.trimIndent(),
            )
            database.execSQL(
                "CREATE INDEX index_gift_outbox_ownerUserId_isSent_createdAtEpochMs ON gift_outbox(ownerUserId, isSent, createdAtEpochMs)",
            )
        }
    }

    /** Keep gift sender details and sender metadata required by public gift feed events. */
    val MIGRATION_20_21 = object : Migration(20, 21) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE user_gifts ADD COLUMN senderName TEXT")
            database.execSQL("ALTER TABLE feed_events ADD COLUMN relatedUserFirebaseUid TEXT")
            database.execSQL("ALTER TABLE feed_events ADD COLUMN relatedUserName TEXT")
        }
    }

    /** Persist cloud cleanup work before a linked local profile is removed. */
    val MIGRATION_21_22 = object : Migration(21, 22) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                """
                CREATE TABLE pending_profile_deletions (
                    firebaseUid TEXT NOT NULL PRIMARY KEY,
                    googleSubject TEXT,
                    status TEXT NOT NULL,
                    requestedAtEpochMs INTEGER NOT NULL
                )
                """.trimIndent(),
            )
            database.execSQL(
                "CREATE INDEX index_pending_profile_deletions_googleSubject ON pending_profile_deletions(googleSubject)",
            )
        }
    }


    /** Store dates protected by patches and the idempotent per-profile processing cursor. */
    val MIGRATION_22_23 = object : Migration(22, 23) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                """
                CREATE TABLE streak_patch_days (
                    userId TEXT NOT NULL,
                    date INTEGER NOT NULL,
                    appliedAtEpochMs INTEGER NOT NULL,
                    PRIMARY KEY(userId, date)
                )
                """.trimIndent(),
            )
            database.execSQL(
                "CREATE INDEX index_streak_patch_days_userId_date ON streak_patch_days(userId, date)",
            )
            database.execSQL(
                """
                CREATE TABLE streak_patch_state (
                    userId TEXT NOT NULL PRIMARY KEY,
                    lastProcessedDate INTEGER NOT NULL,
                    isStreakActive INTEGER NOT NULL
                )
                """.trimIndent(),
            )
        }
    }

}

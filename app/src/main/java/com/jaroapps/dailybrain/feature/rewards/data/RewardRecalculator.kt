package com.jaroapps.dailybrain.feature.rewards.data

import com.jaroapps.dailybrain.common.domain.model.ExerciseType
import com.jaroapps.dailybrain.feature.rewards.domain.model.EndorphinEconomy
import com.jaroapps.dailybrain.feature.rewards.domain.model.WeeklyAchievementRules
import com.jaroapps.dailybrain.core.database.AppDatabase
import com.jaroapps.dailybrain.core.database.entity.FeedEventEntity
import com.jaroapps.dailybrain.core.database.entity.RewardLedgerEntity
import com.jaroapps.dailybrain.core.database.entity.TrainingSessionEntity
import com.jaroapps.dailybrain.core.database.entity.UserBadgeEntity
import com.jaroapps.dailybrain.core.utils.localDateAtEpochMillis
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters
import java.time.DayOfWeek
import java.time.temporal.WeekFields

/**
 * Rebuilds earned rewards and achievement events from the training history.
 * Deterministic recalculation also handles deletion of the first workout of a day/week.
 */
object RewardRecalculator {
    private const val CATEGORY_EARNED = "EARNED"
    private const val SYNC_LOCAL_ONLY = "LOCAL_ONLY"
    private const val SYNC_PENDING = "PENDING_UPSERT"
    private const val SYNC_DELETE = "PENDING_DELETE"
    private const val SYNCED = "SYNCED"

    private val weekFields = WeekFields.ISO
    private val knownNonArithmeticTypes = setOf(
        ExerciseType.MEMORY_WORDS.storageValue,
        ExerciseType.STROOP_TEST.storageValue,
        ExerciseType.COUNTING_TRAINING.storageValue,
    )

    suspend fun recalculate(database: AppDatabase, localAccountId: String) {
        val sessions = database.trainingSessionDao().getAll(localAccountId)
        val profile = database.userProfileDao().getProfile(localAccountId) ?: return
        // Derive reward days from the actual completion timestamp in the device's current
        // local zone. This also repairs legacy rows whose stored date used a different zone.
        val ordered = sessions.sortedWith(compareBy<TrainingSessionEntity>({ it.finishedAtEpochMs }, { it.id }))
        val dated = ordered.map { session ->
            localDateAtEpochMillis(session.finishedAtEpochMs) to session
        }

        val drafts = mutableListOf<RewardDraft>()
        val desiredBadges = linkedMapOf<String, UserBadgeEntity>()
        val desiredEvents = linkedMapOf<String, FeedEventEntity>()
        val sessionsByDate = dated.groupBy({ it.first }, { it.second })
        // Every completed workout has a session row. Use the same local timestamp dates
        // for active days, daily XP, streaks, and monthly achievements.
        val activeTrainingDates = dated.mapTo(mutableSetOf()) { it.first }
        val existingLocalEvents = database.feedEventDao().getLocalEvents(localAccountId).associateBy { it.eventId }

        fun addReward(
            key: String,
            xp: Int,
            endorphins: Int,
            reason: String,
            trigger: TrainingSessionEntity,
            periodKey: String? = null,
        ) {
            if (xp == 0 && endorphins == 0) return
            val date = dated.firstOrNull { it.second.id == trigger.id }?.first ?: return
            drafts += RewardDraft(
                entity = RewardLedgerEntity(
                    userId = localAccountId,
                    rewardKey = key,
                    deltaXp = xp,
                    deltaEndorphins = endorphins,
                    category = CATEGORY_EARNED,
                    reason = reason,
                    sourceId = trigger.id,
                    periodKey = periodKey,
                    occurredAtEpochMs = trigger.finishedAtEpochMs,
                    isActive = true,
                ),
                date = date,
            )
        }

        fun addEvent(
            key: String,
            type: String,
            trigger: TrainingSessionEntity,
            periodKey: String? = null,
            exerciseType: String? = null,
            valueLong: Long? = null,
            valueInt: Int? = null,
            accuracyPercent: Double? = null,
        ) {
            val firebaseUid = profile.firebaseUid
            val stableAuthorId = firebaseUid ?: localAccountId
            val eventId = "$stableAuthorId:$key"
            desiredEvents[eventId] = FeedEventEntity(
                localAccountId = localAccountId,
                eventId = eventId,
                authorFirebaseUid = firebaseUid,
                authorName = profile.displayName?.trim()?.takeIf(String::isNotEmpty) ?: profile.generatedAlias,
                authorUsername = profile.username,
                eventType = type,
                occurredAtEpochMs = trigger.finishedAtEpochMs,
                periodKey = periodKey,
                exerciseType = exerciseType,
                valueLong = valueLong,
                valueInt = valueInt,
                accuracyPercent = accuracyPercent,
                isLocal = true,
                isRevoked = false,
                syncState = if (firebaseUid == null) SYNC_LOCAL_ONLY else SYNC_PENDING,
            )
        }

        fun addBadge(
            badgeId: String,
            badgeType: String,
            periodKey: String,
            trigger: TrainingSessionEntity,
        ) {
            desiredBadges[badgeId] = UserBadgeEntity(
                ownerUserId = localAccountId,
                badgeId = badgeId,
                badgeType = badgeType,
                periodKey = periodKey,
                awardedAtEpochMs = trigger.finishedAtEpochMs,
                triggerWorkoutId = trigger.id,
                isRevoked = false,
            )
        }

        // Ordinary exercise completions award XP only; their endorphin delta is always zero.
        // Endorphins are reserved for eligible achievements (weekly/monthly milestones and records).
        // Arithmetic: 50 XP for the first result of each local day, 10 XP for later results.
        dated.filter { ExerciseType.fromStorage(it.second.exerciseType) == ExerciseType.ARITHMETIC }
            .groupBy({ it.first }, { it.second })
            .forEach { (date, daySessions) ->
                daySessions.sortedBy { it.finishedAtEpochMs }.forEachIndexed { index, session ->
                    addReward(
                        key = "arithmetic:${session.id}",
                        xp = if (index == 0) 50 else 10,
                        endorphins = 0,
                        reason = if (index == 0) "FIRST_ARITHMETIC_OF_DAY" else "REPEAT_ARITHMETIC_OF_DAY",
                        trigger = session,
                        periodKey = date.toString(),
                    )
                }
            }

        // First completion of each non-arithmetic exercise type in an ISO week gives 50 XP.
        dated.filter {
            val type = ExerciseType.fromStorage(it.second.exerciseType)
            type != null && type != ExerciseType.ARITHMETIC
        }.groupBy { (date, session) ->
            val weekStart = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val weekKey = "${weekStart.get(weekFields.weekBasedYear())}-W${weekStart.get(weekFields.weekOfWeekBasedYear()).toString().padStart(2, '0')}"
            "$weekKey:${session.exerciseType}"
        }.values.forEach { weekTypeSessions ->
            val firstEntry = weekTypeSessions.minByOrNull { it.second.finishedAtEpochMs } ?: return@forEach
            val date = firstEntry.first
            val first = firstEntry.second
            val weekStart = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val weekKey = "${weekStart.get(weekFields.weekBasedYear())}-W${weekStart.get(weekFields.weekOfWeekBasedYear()).toString().padStart(2, '0')}"
            addReward(
                key = "other_exercise:${first.exerciseType}:$weekKey",
                xp = 50,
                endorphins = 0,
                reason = "FIRST_EXERCISE_TYPE_OF_WEEK",
                trigger = first,
                periodKey = weekKey,
            )
        }

        // Weekly streak: any completed exercise marks the day as active.
        // These dates use the same local completion timestamps as daily XP.
        var firstStreakCompletedAt: Long? = null
        WeeklyAchievementRules.weeklyStreakCompletionDates(activeTrainingDates).forEach { date ->
            // The first workout of the seventh active day is when the streak is completed.
            val trigger = sessionsByDate[date]?.minByOrNull { it.finishedAtEpochMs }
                ?: return@forEach
            val periodKey = date.toString()
            val key = "weekly_streak:$periodKey"
            addReward(key, 100, EndorphinEconomy.WEEKLY_STREAK_REWARD, "WEEKLY_STREAK", trigger, periodKey)
            addBadge(key, "WEEKLY_STREAK", periodKey, trigger)
            addEvent(key, "WEEKLY_STREAK", trigger, periodKey)
            if (firstStreakCompletedAt == null) firstStreakCompletedAt = trigger.finishedAtEpochMs
        }

        // Weekly challenge: arithmetic is required on each of the seven dates in
        // this ISO week, plus all three additional exercise types within the same week.
        // The same eligibility predicate gates the XP and endorphin reward entries.
        val sessionsByWeek = dated.groupBy { (date, _) ->
            date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        }
        sessionsByWeek.forEach { (weekStart, weekItems) ->
            val arithmeticSessionsByDate = weekItems
                .filter { ExerciseType.fromStorage(it.second.exerciseType) == ExerciseType.ARITHMETIC }
                .groupBy({ it.first }, { it.second })
            val arithmeticDates = arithmeticSessionsByDate.keys
            val completedAdditionalTypes = weekItems
                .map { (_, session) -> session.exerciseType }
                .filter { it in knownNonArithmeticTypes }
                .toSet()
            val hasWeeklyChallenge = WeeklyAchievementRules.isWeeklyChallengeCompleted(
                weekStart = weekStart,
                arithmeticTrainingDates = arithmeticDates,
                completedExerciseTypes = completedAdditionalTypes,
                requiredAdditionalExerciseTypes = knownNonArithmeticTypes,
            )
            if (hasWeeklyChallenge) {
                // Timestamp the achievement at the last condition that was satisfied:
                // one arithmetic workout on each day and one of every additional exercise.
                val qualifyingSessions = mutableListOf<TrainingSessionEntity>()
                for (offset in 0 until WeeklyAchievementRules.REQUIRED_WEEK_DAYS) {
                    arithmeticSessionsByDate[weekStart.plusDays(offset.toLong())]
                        ?.minByOrNull { it.finishedAtEpochMs }
                        ?.let(qualifyingSessions::add)
                }
                knownNonArithmeticTypes.forEach { exerciseType ->
                    weekItems.asSequence()
                        .filter { it.second.exerciseType == exerciseType }
                        .minByOrNull { it.second.finishedAtEpochMs }
                        ?.second
                        ?.let(qualifyingSessions::add)
                }
                val trigger = qualifyingSessions.maxByOrNull { it.finishedAtEpochMs } ?: return@forEach
                val weekKey = "${weekStart.get(weekFields.weekBasedYear())}-W${weekStart.get(weekFields.weekOfWeekBasedYear()).toString().padStart(2, '0')}"
                val key = "weekly_challenge:$weekKey"
                addReward(key, 100, EndorphinEconomy.WEEKLY_CHALLENGE_REWARD, "WEEKLY_CHALLENGE", trigger, weekKey)
                addBadge(key, "WEEKLY_CHALLENGE", weekKey, trigger)
                addEvent(key, "WEEKLY_CHALLENGE", trigger, weekKey)
            }
        }

        // A monthly series, like the calendar day streak, accepts any exercise type.
        // Evaluate it from the same local completed-session dates used by the daily streak.
        val trainingDatesByMonth = activeTrainingDates.groupBy { YearMonth.from(it) }
        trainingDatesByMonth.forEach { (month, trainedDates) ->
            val allDates = (1..month.lengthOfMonth()).map { month.atDay(it) }.toSet()
            if (trainedDates.containsAll(allDates)) {
                val lastDay = month.atEndOfMonth()
                val trigger = sessionsByDate[lastDay]?.minByOrNull { it.finishedAtEpochMs } ?: return@forEach
                val monthKey = month.toString()
                val badgeType = "MONTH_${month.monthValue.toString().padStart(2, '0')}"
                val key = "monthly_streak:$monthKey"
                addReward(key, 500, EndorphinEconomy.MONTHLY_STREAK_REWARD, "MONTHLY_STREAK", trigger, monthKey)
                addBadge(key, badgeType, monthKey, trigger)
                addEvent(key, "MONTHLY_STREAK", trigger, monthKey, valueInt = month.monthValue)
            }
        }

        // The first seven arithmetic sessions are practice. New records require >=95% accuracy
        // and can be earned only after the first completed weekly streak.
        val arithmeticSessions = dated.filter {
            ExerciseType.fromStorage(it.second.exerciseType) == ExerciseType.ARITHMETIC
        }.sortedBy { it.second.finishedAtEpochMs }
        var bestEligibleTime: Long? = null
        arithmeticSessions.forEachIndexed { index, (date, session) ->
            if (index < 7 || session.accuracyPercent < 95.0 || session.durationMs <= 0L) return@forEachIndexed
            val streakCompletedAt = firstStreakCompletedAt ?: return@forEachIndexed
            if (session.finishedAtEpochMs < streakCompletedAt) return@forEachIndexed
            val previousBest = bestEligibleTime
            if (previousBest == null || session.durationMs < previousBest) {
                bestEligibleTime = session.durationMs
                val key = "arithmetic_record:${session.id}"
                addReward(key, 100, EndorphinEconomy.ARITHMETIC_RECORD_REWARD, "ARITHMETIC_PERSONAL_RECORD", session, date.toString())
                addEvent(
                    key = key,
                    type = "PERSONAL_RECORD",
                    trigger = session,
                    periodKey = date.toString(),
                    exerciseType = ExerciseType.ARITHMETIC.storageValue,
                    valueLong = session.durationMs,
                    accuracyPercent = session.accuracyPercent,
                )
            }
        }

        // Weekly XP milestones count ordinary XP and achievement bonuses, but grant no extra XP.
        val milestones = listOf(250, 500)
        drafts.groupBy { draft ->
            draft.date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        }.forEach { (weekStart, weekDrafts) ->
            var runningXp = 0
            val emitted = mutableSetOf<Int>()
            weekDrafts.sortedBy { it.entity.occurredAtEpochMs }.forEach { draft ->
                runningXp += draft.entity.deltaXp
                milestones.forEach thresholdLoop@{ threshold ->
                    if (runningXp >= threshold && threshold !in emitted) {
                        emitted += threshold
                        val trigger = ordered.firstOrNull { it.id == draft.entity.sourceId } ?: return@thresholdLoop
                        val weekKey = "${weekStart.get(weekFields.weekBasedYear())}-W${weekStart.get(weekFields.weekOfWeekBasedYear()).toString().padStart(2, '0')}"
                        addEvent(
                            key = "weekly_xp:$weekKey:$threshold",
                            type = "WEEKLY_XP_MILESTONE",
                            trigger = trigger,
                            periodKey = weekKey,
                            valueInt = threshold,
                        )
                    }
                }
            }
        }

        val ledgerDao = database.rewardLedgerDao()
        val existingLedger = ledgerDao.getForUser(localAccountId)
        val desiredRewardKeys = drafts.map { it.entity.rewardKey }.toSet()
        val updatedLedger = mutableListOf<RewardLedgerEntity>()
        existingLedger.filter {
            it.category == CATEGORY_EARNED && it.isActive && it.rewardKey !in desiredRewardKeys
        }.forEach { updatedLedger += it.copy(isActive = false) }
        drafts.forEach { updatedLedger += it.entity }
        if (updatedLedger.isNotEmpty()) ledgerDao.upsertAll(updatedLedger)

        val badgeDao = database.userBadgeDao()
        val existingBadges = badgeDao.getForUser(localAccountId).associateBy { it.badgeId }
        val allBadges = mutableListOf<UserBadgeEntity>()
        existingBadges.values.filter { it.badgeId !in desiredBadges.keys && !it.isRevoked }
            .forEach { allBadges += it.copy(isRevoked = true) }
        desiredBadges.values.forEach { badge ->
            val previous = existingBadges[badge.badgeId]
            allBadges += if (previous == null || previous.triggerWorkoutId != badge.triggerWorkoutId) {
                badge
            } else {
                badge.copy(awardedAtEpochMs = previous.awardedAtEpochMs, isRevoked = false)
            }
        }
        if (allBadges.isNotEmpty()) badgeDao.upsertAll(allBadges)

        val feedDao = database.feedEventDao()
        val reconciledEvents = mutableListOf<FeedEventEntity>()
        existingLocalEvents.values.filter { it.eventId !in desiredEvents.keys && !it.isRevoked }.forEach { previous ->
            reconciledEvents += previous.copy(
                isRevoked = true,
                syncState = if (previous.authorFirebaseUid != null && previous.syncState != SYNC_LOCAL_ONLY) {
                    SYNC_DELETE
                } else {
                    SYNC_LOCAL_ONLY
                },
            )
        }
        desiredEvents.values.forEach { event ->
            val previous = existingLocalEvents[event.eventId]
            val publishedContentUnchanged = previous != null &&
                !previous.isRevoked &&
                previous.syncState == SYNCED &&
                previous.authorName == event.authorName &&
                previous.authorUsername == event.authorUsername &&
                previous.eventType == event.eventType &&
                previous.occurredAtEpochMs == event.occurredAtEpochMs &&
                previous.periodKey == event.periodKey &&
                previous.exerciseType == event.exerciseType &&
                previous.valueLong == event.valueLong &&
                previous.valueInt == event.valueInt &&
                previous.accuracyPercent == event.accuracyPercent
            reconciledEvents += if (publishedContentUnchanged) {
                event.copy(syncState = SYNCED)
            } else {
                event
            }
        }
        if (reconciledEvents.isNotEmpty()) feedDao.upsertAll(reconciledEvents)

        val xpBalance = ledgerDao.getXpBalance(localAccountId).coerceAtLeast(0)
        // Revoking an earned reward must preserve a negative balance when its endorphins were spent.
        val endorphinBalance = ledgerDao.getEndorphinBalance(localAccountId)
        if (
            profile.xpPoints != xpBalance ||
            profile.endorphinBalance != endorphinBalance ||
            !profile.rewardsInitialized
        ) {
            database.userProfileDao().save(
                profile.copy(
                    xpPoints = xpBalance,
                    endorphinBalance = endorphinBalance,
                    rewardsInitialized = true,
                    updatedAtEpochMs = System.currentTimeMillis(),
                ),
            )
        }
    }

    private data class RewardDraft(
        val entity: RewardLedgerEntity,
        val date: LocalDate,
    )
}

package com.jaroapps.dailybrain.common.data.repository

import androidx.room.withTransaction
import com.jaroapps.dailybrain.common.domain.model.ExerciseType
import com.jaroapps.dailybrain.common.domain.model.TrainingSession
import com.jaroapps.dailybrain.common.domain.model.TrainingStreakCalculator
import com.jaroapps.dailybrain.common.domain.repository.CompletedTrainingRepository
import com.jaroapps.dailybrain.core.database.AppDatabase
import com.jaroapps.dailybrain.core.database.ActiveUserProvider
import com.jaroapps.dailybrain.core.database.StatisticsRules
import com.jaroapps.dailybrain.core.database.entity.ExerciseStatisticsEntity
import com.jaroapps.dailybrain.core.database.entity.OverallStatisticsEntity
import com.jaroapps.dailybrain.core.database.entity.TrainingDayEntity
import com.jaroapps.dailybrain.core.database.entity.TrainingSessionEntity
import com.jaroapps.dailybrain.core.utils.formatMillisToDate
import com.jaroapps.dailybrain.feature.rewards.data.RewardRecalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flatMapLatest
import java.time.LocalDate

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class CompletedTrainingRepositoryImpl(
    private val database: AppDatabase,
    private val activeUserProvider: ActiveUserProvider,
) : CompletedTrainingRepository {

    override suspend fun saveCompletedTraining(session: TrainingSession) {
        val date = formatMillisToDate(session.finishedAtEpochMs)
        val userId = activeUserProvider.currentUserId()

        database.withTransaction {
            database.trainingSessionDao().insert(
                TrainingSessionEntity(
                    id = session.startedAtEpochMs,
                    userId = userId,
                    exerciseType = session.exerciseType,
                    startedAtEpochMs = session.startedAtEpochMs,
                    finishedAtEpochMs = session.finishedAtEpochMs,
                    date = date,
                    durationMs = session.durationMs,
                    accuracyPercent = session.accuracyPercent,
                    taskCount = session.taskCount,
                    correctCount = session.correctCount,
                )
            )

            updateTrainingDayCount(userId, date)
            updateOverallStatistics(session, date, userId)
            updateExerciseStatistics(session, userId)
            updateDailyExerciseResult(session, date, userId)
            RewardRecalculator.recalculate(database, userId)
        }
    }


    override suspend fun initializeRewardState() {
        val userId = activeUserProvider.currentUserId()
        database.withTransaction {
            val profile = database.userProfileDao().getProfile(userId)
            if (profile?.rewardsInitialized != true) {
                RewardRecalculator.recalculate(database, userId)
            }
        }
    }

    override suspend fun recalculateDailyExerciseResult(date: Int) {
        val userId = activeUserProvider.currentUserId()
        database.withTransaction {
            recalculateDailyExerciseResultInTransaction(date, userId)
        }
    }

    private suspend fun recalculateDailyExerciseResultInTransaction(date: Int, userId: String) {
        val dao = database.dailyExerciseResultDao()
        dao.deleteByDate(userId, date)
        database.trainingSessionDao().getAllByDate(userId, date).forEach { entity ->
            updateDailyExerciseResult(entity.toDomain(), date, userId)
        }
    }

    override suspend fun deleteCompletedTraining(id: Long): Boolean {
        val userId = activeUserProvider.currentUserId()
        return database.withTransaction {
            val sessionDao = database.trainingSessionDao()
            val deletedSession = sessionDao.getById(id, userId) ?: return@withTransaction false
            if (sessionDao.deleteById(id, userId) == 0) return@withTransaction false

            // Update only the affected calendar day and preserve its stored historical date key.
            updateTrainingDayCount(userId, deletedSession.date)

            val remainingSessions = sessionDao.getAll(userId)
            val statisticsDao = database.statisticsDao()
            statisticsDao.deleteOverall(userId)
            statisticsDao.deleteAllExerciseStatistics(userId)
            remainingSessions.forEach { entity ->
                val training = entity.toDomain()
                updateOverallStatistics(training, entity.date, userId, recalculateStreak = false)
                updateExerciseStatistics(training, userId)
            }
            updateOverallStreakStatistics(userId)

            recalculateDailyExerciseResultInTransaction(deletedSession.date, userId)
            RewardRecalculator.recalculate(database, userId)

            true
        }
    }

    private suspend fun updateDailyExerciseResult(session: TrainingSession, date: Int, userId: String) {
        val dao = database.dailyExerciseResultDao()
        val duration = session.durationMs.coerceAtLeast(0L)
        when (ExerciseType.fromStorage(session.exerciseType)) {
            ExerciseType.ARITHMETIC -> {
                if (session.accuracyPercent >= StatisticsRules.ARITHMETIC_BEST_TIME_MIN_ACCURACY_PERCENT) {
                    dao.recordArithmeticBest(userId, date, duration)
                }
            }
            ExerciseType.MEMORY_WORDS -> dao.recordMemoryBest(userId, date, session.correctCount.coerceAtLeast(0))
            ExerciseType.STROOP_TEST -> dao.recordStroopBest(userId, date, duration)
            ExerciseType.COUNTING_TRAINING -> dao.recordCountingBest(userId, date, duration)
            null -> Unit
        }
    }

    private suspend fun updateTrainingDayCount(userId: String, date: Int) {
        val count = database.trainingSessionDao().countByDate(userId, date)
        val updatedRows = database.trainingDayDao().updateExerciseCount(
            userId = userId,
            date = date,
            exerciseCount = count,
        )

        when {
            count == 0 -> database.trainingDayDao().deleteByDate(userId, date)
            updatedRows == 0 -> database.trainingDayDao().insertAll(
                listOf(TrainingDayEntity(userId = userId, date = date, exerciseCount = count)),
            )
        }
    }

    private suspend fun updateOverallStatistics(
        session: TrainingSession,
        date: Int,
        userId: String,
        recalculateStreak: Boolean = true,
    ) {
        val dao = database.statisticsDao()
        val previous = dao.getOverall(userId)
        dao.saveOverall(
            OverallStatisticsEntity(
                userId = userId,
                totalTrainingCount = (previous?.totalTrainingCount ?: 0) + 1,
                totalDurationMs = (previous?.totalDurationMs ?: 0L) +
                    session.durationMs.coerceAtLeast(0L),
                currentStreakDays = previous?.currentStreakDays ?: 0,
                bestStreakDays = previous?.bestStreakDays ?: 0,
                lastTrainingDate = date,
                updatedAtEpochMs = System.currentTimeMillis(),
            ),
        )
        if (recalculateStreak) updateOverallStreakStatistics(userId)
    }

    private suspend fun updateOverallStreakStatistics(userId: String) {
        val dao = database.statisticsDao()
        val previous = dao.getOverall(userId) ?: return
        val trainingDates = database.trainingDayDao().getAll(userId).map { it.date }
        val patchedDates = database.streakPatchDao().getDays(userId).map { it.date }
        val activeDates = TrainingStreakCalculator.parseDateKeys(trainingDates + patchedDates)
        val streak = TrainingStreakCalculator.calculate(activeDates, LocalDate.now())
        dao.saveOverall(
            previous.copy(
                currentStreakDays = streak.currentDays,
                bestStreakDays = streak.bestDays,
                updatedAtEpochMs = System.currentTimeMillis(),
            ),
        )
    }

    private suspend fun updateExerciseStatistics(session: TrainingSession, userId: String) {
        val dao = database.statisticsDao()
        val previous = dao.getExerciseStatistics(userId, session.exerciseType)
        val exerciseType = ExerciseType.fromStorage(session.exerciseType)
        val duration = session.durationMs.coerceAtLeast(0L)

        val durationEligible = session.exerciseType != ExerciseType.ARITHMETIC.storageValue ||
            session.accuracyPercent >= StatisticsRules.ARITHMETIC_BEST_TIME_MIN_ACCURACY_PERCENT
        val candidateBestDuration = if (durationEligible) duration else null
        val bestDuration = if (candidateBestDuration == null) {
            previous?.bestDurationMs
        } else {
            val previousBest = previous?.bestDurationMs
            if (previousBest == null) candidateBestDuration
            else minOf(previousBest, candidateBestDuration)
        }

        val accuracyIsMeasured = session.taskCount > 0 &&
            (exerciseType == ExerciseType.ARITHMETIC || exerciseType == ExerciseType.MEMORY_WORDS)
        val candidateAccuracy = session.accuracyPercent.coerceIn(0.0, 100.0)
            .takeIf { accuracyIsMeasured }
        val bestAccuracy = if (candidateAccuracy == null) {
            previous?.bestAccuracyPercent
        } else {
            val previousBest = previous?.bestAccuracyPercent
            if (previousBest == null) candidateAccuracy
            else maxOf(previousBest, candidateAccuracy)
        }

        val candidateCorrectCount = session.correctCount.coerceAtLeast(0)
            .takeIf { exerciseType == ExerciseType.MEMORY_WORDS }
        val bestCorrectCount = if (candidateCorrectCount == null) {
            previous?.bestCorrectCount
        } else {
            val previousBest = previous?.bestCorrectCount
            if (previousBest == null) candidateCorrectCount
            else maxOf(previousBest, candidateCorrectCount)
        }

        dao.saveExerciseStatistics(
            ExerciseStatisticsEntity(
                userId = userId,
                exerciseType = session.exerciseType,
                trainingCount = (previous?.trainingCount ?: 0) + 1,
                totalDurationMs = (previous?.totalDurationMs ?: 0L) + duration,
                bestDurationMs = bestDuration,
                bestAccuracyPercent = bestAccuracy,
                bestCorrectCount = bestCorrectCount,
                lastTrainingAtEpochMs = maxOf(
                    previous?.lastTrainingAtEpochMs ?: 0L,
                    session.finishedAtEpochMs,
                ),
            )
        )
    }

    override fun observeAllCompletedTrainings(): Flow<List<TrainingSession>> =
        activeUserProvider.observeUserId().flatMapLatest { userId ->
            database.trainingSessionDao().observeAll(userId).map { sessions -> sessions.map { it.toDomain() } }
        }

    override fun observeCompletedTrainingsByDate(date: Int): Flow<List<TrainingSession>> =
        activeUserProvider.observeUserId().flatMapLatest { userId ->
            database.trainingSessionDao().observeByDate(userId, date).map { sessions -> sessions.map { it.toDomain() } }
        }

    override fun observeActiveTrainingDates(): Flow<Set<Int>> =
        activeUserProvider.observeUserId().flatMapLatest { userId -> database.trainingDayDao().observeAll(userId).map { days -> days.map { it.date }.toSet() } }

    override fun observeTrainingDaysBetween(startDate: Int, endDate: Int): Flow<Set<Int>> =
        activeUserProvider.observeUserId().flatMapLatest { userId -> database.trainingDayDao().observeBetweenDates(userId, startDate, endDate).map { days -> days.map { it.date }.toSet() } }

    override fun observeTotalTrainingCount(): Flow<Int> =
        activeUserProvider.observeUserId().flatMapLatest { userId -> database.trainingSessionDao().observeTotalCount(userId) }

    private fun TrainingSessionEntity.toDomain() = TrainingSession(
        id = id,
        exerciseType = exerciseType,
        startedAtEpochMs = startedAtEpochMs,
        finishedAtEpochMs = finishedAtEpochMs,
        durationMs = durationMs,
        accuracyPercent = accuracyPercent,
        taskCount = taskCount,
        correctCount = correctCount,
    )
}

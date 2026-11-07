package com.jaroapps.dailybrain.common.data.repository

import androidx.room.withTransaction
import com.jaroapps.dailybrain.common.domain.model.TrainingStreakCalculator
import com.jaroapps.dailybrain.common.domain.repository.StreakPatchRepository
import com.jaroapps.dailybrain.core.database.ActiveUserProvider
import com.jaroapps.dailybrain.core.database.AppDatabase
import com.jaroapps.dailybrain.core.database.entity.StreakPatchDayEntity
import com.jaroapps.dailybrain.core.database.entity.StreakPatchStateEntity
import com.jaroapps.dailybrain.feature.rewards.data.RewardRecalculator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class StreakPatchRepositoryImpl(
    private val database: AppDatabase,
    private val activeUserProvider: ActiveUserProvider,
) : StreakPatchRepository {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observePatchedDates(): Flow<Set<Int>> =
        activeUserProvider.observeUserId().flatMapLatest { userId ->
            database.streakPatchDao().observeDays(userId).map { days ->
                days.mapTo(mutableSetOf()) { it.date }
            }
        }

    override suspend fun processMissedDays(): Int {
        val userId = activeUserProvider.currentUserId()
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)
        val yesterdayKey = yesterday.toDateKey()

        return database.withTransaction {
            val profileDao = database.userProfileDao()
            val profile = profileDao.getProfile(userId) ?: return@withTransaction 0
            val patchDao = database.streakPatchDao()
            val trainingDateKeys = database.trainingDayDao().getAll(userId).mapTo(mutableSetOf()) { it.date }
            val patchDays = patchDao.getDays(userId).mapTo(mutableSetOf()) { it.date }

            val persistedState = patchDao.getState(userId)
            val state = persistedState ?: run {
                // On the first run after upgrading, begin from the latest real training.
                // Earlier gaps are not reconstructed; only days after the latest training can be protected.
                val latestTrainingDate = trainingDateKeys.maxOrNull()
                StreakPatchStateEntity(
                    userId = userId,
                    lastProcessedDate = latestTrainingDate ?: yesterdayKey,
                    isStreakActive = latestTrainingDate != null,
                )
            }

            var cursor = runCatching {
                LocalDate.parse(state.lastProcessedDate.toString(), DateTimeFormatter.BASIC_ISO_DATE)
            }.getOrElse { yesterday }
            var streakActive = state.isStreakActive
            var remainingPatches = profile.patchCount.coerceAtLeast(0)
            var appliedCount = 0

            while (cursor.isBefore(yesterday)) {
                cursor = cursor.plusDays(1)
                val dateKey = cursor.toDateKey()
                when {
                    dateKey in trainingDateKeys -> streakActive = true
                    dateKey in patchDays -> streakActive = true
                    streakActive && remainingPatches > 0 -> {
                        val inserted = patchDao.insertDayIfAbsent(
                            StreakPatchDayEntity(
                                userId = userId,
                                date = dateKey,
                                appliedAtEpochMs = System.currentTimeMillis(),
                            ),
                        )
                        if (inserted != -1L) {
                            patchDays += dateKey
                            remainingPatches--
                            appliedCount++
                        }
                    }
                    else -> streakActive = false
                }
            }

            patchDao.saveState(
                StreakPatchStateEntity(
                    userId = userId,
                    lastProcessedDate = maxOf(state.lastProcessedDate, yesterdayKey),
                    isStreakActive = streakActive,
                ),
            )

            if (appliedCount > 0) {
                profileDao.save(
                    profile.copy(
                        patchCount = remainingPatches,
                        updatedAtEpochMs = System.currentTimeMillis(),
                    ),
                )
                RewardRecalculator.recalculate(database, userId)
                updateCachedStreak(userId, today)
            }
            appliedCount
        }
    }

    private suspend fun updateCachedStreak(userId: String, today: LocalDate) {
        val previous = database.statisticsDao().getOverall(userId) ?: return
        val trained = database.trainingDayDao().getAll(userId).map { it.date }
        val patched = database.streakPatchDao().getDays(userId).map { it.date }
        val streak = TrainingStreakCalculator.calculate(
            activeDates = TrainingStreakCalculator.parseDateKeys(trained + patched),
            today = today,
        )
        database.statisticsDao().saveOverall(
            previous.copy(
                currentStreakDays = streak.currentDays,
                bestStreakDays = streak.bestDays,
                updatedAtEpochMs = System.currentTimeMillis(),
            ),
        )
    }

    private fun LocalDate.toDateKey(): Int =
        format(DateTimeFormatter.BASIC_ISO_DATE).toInt()
}

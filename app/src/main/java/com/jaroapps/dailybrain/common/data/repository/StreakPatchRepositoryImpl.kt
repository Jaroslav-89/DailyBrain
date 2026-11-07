package com.jaroapps.dailybrain.common.data.repository

import androidx.room.withTransaction
import com.jaroapps.dailybrain.common.domain.model.TrainingStreakCalculator
import com.jaroapps.dailybrain.common.domain.model.StreakPatchProcessingSummary
import com.jaroapps.dailybrain.common.domain.model.StreakPatchRules
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

    override suspend fun processMissedDays(): StreakPatchProcessingSummary {
        val userId = activeUserProvider.currentUserId()
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)

        return database.withTransaction {
            val profileDao = database.userProfileDao()
            val profile = profileDao.getProfile(userId)
                ?: return@withTransaction StreakPatchProcessingSummary()
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
                    lastProcessedDate = latestTrainingDate ?: yesterday.toDateKey(),
                    isStreakActive = latestTrainingDate != null,
                )
            }

            val cursor = runCatching {
                LocalDate.parse(state.lastProcessedDate.toString(), DateTimeFormatter.BASIC_ISO_DATE)
            }.getOrElse { yesterday }
            val result = StreakPatchRules.process(
                lastProcessedDate = cursor,
                throughDate = yesterday,
                trainingDates = TrainingStreakCalculator.parseDateKeys(trainingDateKeys),
                existingPatchedDates = TrainingStreakCalculator.parseDateKeys(patchDays),
                isStreakActive = state.isStreakActive,
                availablePatches = profile.patchCount,
            )

            val appliedAt = System.currentTimeMillis()
            result.newlyPatchedDates.forEach { date ->
                patchDao.insertDayIfAbsent(
                    StreakPatchDayEntity(
                        userId = userId,
                        date = date.toDateKey(),
                        appliedAtEpochMs = appliedAt,
                    ),
                )
            }
            val appliedCount = result.newlyPatchedDates.size
            val remainingPatches = (profile.patchCount - appliedCount).coerceAtLeast(0)
            patchDao.saveState(
                StreakPatchStateEntity(
                    userId = userId,
                    lastProcessedDate = maxOf(result.lastProcessedDate.toDateKey(), state.lastProcessedDate),
                    isStreakActive = result.isStreakActive,
                ),
            )

            if (appliedCount > 0) {
                profileDao.save(
                    profile.copy(
                        patchCount = remainingPatches,
                        updatedAtEpochMs = appliedAt,
                    ),
                )
                RewardRecalculator.recalculate(database, userId)
            }
            updateCachedStreak(userId, today)

            StreakPatchProcessingSummary(
                missedDayCount = result.missedDates.size,
                usedPatchCount = appliedCount,
                remainingPatchCount = remainingPatches,
                wasStreakActive = result.wasStreakActive,
                isStreakPreserved = result.isPreviousStreakPreserved,
                isStreakActiveAfter = result.isStreakActive ||
                    today.toDateKey() in trainingDateKeys || today.toDateKey() in patchDays,
                lastProcessedDate = result.lastProcessedDate,
                patchesAppliedAtEpochMs = appliedAt.takeIf { appliedCount > 0 },
            )
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

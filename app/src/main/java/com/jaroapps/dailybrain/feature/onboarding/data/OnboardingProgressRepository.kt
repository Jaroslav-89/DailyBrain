package com.jaroapps.dailybrain.feature.onboarding.data

import com.jaroapps.dailybrain.core.database.ActiveUserProvider
import com.jaroapps.dailybrain.core.database.dao.OnboardingProgressDao
import com.jaroapps.dailybrain.core.database.entity.OnboardingProgressEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow

class OnboardingProgressRepository(
    private val dao: OnboardingProgressDao,
    private val activeUserProvider: ActiveUserProvider,
) {
    fun observeActiveProgress(): Flow<OnboardingProgressEntity> =
        activeUserProvider.observeUserId().flatMapLatest { localAccountId ->
            flow {
                ensureProgress(localAccountId)
                emitAll(dao.observeProgress(localAccountId).filterNotNull())
            }
        }

    suspend fun getActiveProgress(): OnboardingProgressEntity =
        ensureProgress(activeUserProvider.currentUserId())

    suspend fun markAppIntroComplete() {
        val current = getActiveProgress()
        dao.save(current.copy(hasSeenAppIntro = true))
    }

    suspend fun markExerciseIntroComplete(exerciseKey: String) {
        val current = getActiveProgress()
        val updated = when (exerciseKey) {
            "arithmetic" -> current.copy(hasSeenArithmeticIntro = true)
            "memory" -> current.copy(hasSeenMemoryIntro = true)
            "counting" -> current.copy(hasSeenCountingIntro = true)
            "stroop" -> current.copy(hasSeenStroopIntro = true)
            else -> error("Unknown exercise key: $exerciseKey")
        }
        dao.save(updated)
    }

    suspend fun deleteProgress(localAccountId: String) {
        dao.deleteProgress(localAccountId)
    }

    private suspend fun ensureProgress(localAccountId: String): OnboardingProgressEntity {
        val existing = dao.getProgress(localAccountId)
        if (existing != null) return existing
        return OnboardingProgressEntity(localAccountId = localAccountId).also { dao.save(it) }
    }
}

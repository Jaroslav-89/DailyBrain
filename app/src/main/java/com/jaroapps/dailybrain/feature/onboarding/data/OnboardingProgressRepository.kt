package com.jaroapps.dailybrain.feature.onboarding.data

import com.jaroapps.dailybrain.core.database.ActiveUserProvider
import com.jaroapps.dailybrain.core.database.dao.OnboardingProgressDao
import com.jaroapps.dailybrain.core.database.entity.OnboardingProgressEntity
import com.jaroapps.dailybrain.feature.onboarding.domain.model.OnboardingProgress
import com.jaroapps.dailybrain.feature.onboarding.domain.repository.OnboardingProgressRepository as OnboardingProgressRepositoryContract
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class OnboardingProgressRepository(
    private val dao: OnboardingProgressDao,
    private val activeUserProvider: ActiveUserProvider,
) : OnboardingProgressRepositoryContract {
    override fun observeActiveProgress(): Flow<OnboardingProgress> =
        activeUserProvider.observeUserId().flatMapLatest { localAccountId ->
            flow {
                ensureProgress(localAccountId)
                emitAll(dao.observeProgress(localAccountId).filterNotNull().map { it.toDomain() })
            }
        }

    override suspend fun getActiveProgress(): OnboardingProgress =
        ensureProgress(activeUserProvider.currentUserId()).toDomain()

    override suspend fun markAppIntroComplete() {
        val current = getActiveProgress()
        dao.save(current.copy(hasSeenAppIntro = true).toEntity())
    }

    override suspend fun markExerciseIntroComplete(exerciseKey: String) {
        val current = getActiveProgress()
        val updated = when (exerciseKey) {
            "arithmetic" -> current.copy(hasSeenArithmeticIntro = true)
            "memory" -> current.copy(hasSeenMemoryIntro = true)
            "counting" -> current.copy(hasSeenCountingIntro = true)
            "stroop" -> current.copy(hasSeenStroopIntro = true)
            else -> error("Unknown exercise key: $exerciseKey")
        }
        dao.save(updated.toEntity())
    }

    override suspend fun deleteProgress(localAccountId: String) {
        dao.deleteProgress(localAccountId)
    }

    private suspend fun ensureProgress(localAccountId: String): OnboardingProgressEntity {
        val existing = dao.getProgress(localAccountId)
        if (existing != null) return existing
        return OnboardingProgressEntity(localAccountId = localAccountId).also { dao.save(it) }
    }
}

private fun OnboardingProgressEntity.toDomain() = OnboardingProgress(
    localAccountId = localAccountId,
    hasSeenAppIntro = hasSeenAppIntro,
    hasSeenArithmeticIntro = hasSeenArithmeticIntro,
    hasSeenMemoryIntro = hasSeenMemoryIntro,
    hasSeenCountingIntro = hasSeenCountingIntro,
    hasSeenStroopIntro = hasSeenStroopIntro,
)

private fun OnboardingProgress.toEntity() = OnboardingProgressEntity(
    localAccountId = localAccountId,
    hasSeenAppIntro = hasSeenAppIntro,
    hasSeenArithmeticIntro = hasSeenArithmeticIntro,
    hasSeenMemoryIntro = hasSeenMemoryIntro,
    hasSeenCountingIntro = hasSeenCountingIntro,
    hasSeenStroopIntro = hasSeenStroopIntro,
)

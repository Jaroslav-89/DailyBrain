package com.jaroapps.dailybrain.core.database

import com.jaroapps.dailybrain.core.database.entity.AppSettingsEntity
import com.jaroapps.dailybrain.core.database.entity.UserProfileEntity
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.distinctUntilChanged
import java.util.UUID
import kotlin.random.Random

/**
 * Resolves the selected local account for data-layer operations.
 * The settings row is initialized lazily so a fresh install can start without a pre-existing profile.
 */
class ActiveUserProvider(
    private val database: AppDatabase,
) {
    private val initializationMutex = Mutex()

    suspend fun currentUserId(): String {
        database.appSettingsDao().getActiveLocalAccountId()?.let { return it }
        return initializationMutex.withLock {
            val settingsDao = database.appSettingsDao()
            settingsDao.getActiveLocalAccountId()?.let { return@withLock it }

        val profileDao = database.userProfileDao()
        val profile = profileDao.getLegacyDefaultProfile() ?: UserProfileEntity(
            localAccountId = UUID.randomUUID().toString(),
            generatedAlias = "brain#${Random.nextInt(100, 1000)}",
            createdAtEpochMs = System.currentTimeMillis(),
            updatedAtEpochMs = System.currentTimeMillis(),
        ).also { profileDao.insertIfAbsent(it) }

        val resolved = profileDao.getProfile(profile.localAccountId) ?: profile
        settingsDao.setActiveAccount(AppSettingsEntity(activeLocalAccountId = resolved.localAccountId))
        resolved.localAccountId
        }
    }

    fun observeUserId(): Flow<String> = flow {
        emit(currentUserId())
        emitAll(database.appSettingsDao().observeActiveLocalAccountId().filterNotNull())
    }.distinctUntilChanged()
}
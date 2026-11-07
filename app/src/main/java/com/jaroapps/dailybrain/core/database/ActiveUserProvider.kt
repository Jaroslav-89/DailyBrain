package com.jaroapps.dailybrain.core.database

import com.jaroapps.dailybrain.core.database.entity.AppSettingsEntity
import com.jaroapps.dailybrain.core.database.entity.UserProfileEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import kotlin.random.Random

/** Resolves the selected local account for data-layer operations. */
class ActiveUserProvider(
    private val database: AppDatabase,
) {
    private val initializationMutex = Mutex()

    suspend fun currentUserId(): String {
        val settingsDao = database.appSettingsDao()
        if (settingsDao.isProfileSelectionRequired() == true) {
            throw ProfileSelectionRequiredException()
        }
        settingsDao.getActiveLocalAccountId()?.takeIf(String::isNotBlank)?.let { return it }

        return initializationMutex.withLock {
            if (settingsDao.isProfileSelectionRequired() == true) {
                throw ProfileSelectionRequiredException()
            }
            settingsDao.getActiveLocalAccountId()?.takeIf(String::isNotBlank)?.let { return@withLock it }

            val profileDao = database.userProfileDao()
            val profile = profileDao.getLegacyDefaultProfile() ?: UserProfileEntity(
                localAccountId = UUID.randomUUID().toString(),
                generatedAlias = "brain#${Random.nextInt(100, 1000)}",
                createdAtEpochMs = System.currentTimeMillis(),
                updatedAtEpochMs = System.currentTimeMillis(),
            ).also { profileDao.insertIfAbsent(it) }

            val resolved = profileDao.getProfile(profile.localAccountId) ?: profile
            settingsDao.setActiveAccount(
                AppSettingsEntity(activeLocalAccountId = resolved.localAccountId),
            )
            resolved.localAccountId
        }
    }

    /**
     * Emits null while profile selection is required. Consumers that switch with flatMapLatest
     * must observe this nullable stream so they stop watching a deleted profile instead of
     * recreating it from an empty Room row.
     */
    fun observeSelectedUserId(): Flow<String?> = flow {
        val initialId = try {
            currentUserId()
        } catch (_: ProfileSelectionRequiredException) {
            null
        }
        emit(initialId)

        emitAll(
            combine(
                database.appSettingsDao().observeActiveLocalAccountId(),
                database.appSettingsDao().observeProfileSelectionRequired(),
            ) { localAccountId, selectionRequired ->
                localAccountId?.takeIf { selectionRequired != true && it.isNotBlank() }
            },
        )
    }.distinctUntilChanged()

    fun observeUserId(): Flow<String> = observeSelectedUserId().filterNotNull()
}

class ProfileSelectionRequiredException : IllegalStateException(
    "A local profile must be selected before accessing profile data.",
)
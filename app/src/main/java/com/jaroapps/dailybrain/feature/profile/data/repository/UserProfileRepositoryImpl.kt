package com.jaroapps.dailybrain.feature.profile.data.repository

import androidx.room.withTransaction
import com.jaroapps.dailybrain.core.database.AppDatabase
import com.jaroapps.dailybrain.core.database.ActiveUserProvider
import com.jaroapps.dailybrain.core.database.entity.UserProfileEntity
import com.jaroapps.dailybrain.core.database.entity.AppSettingsEntity
import com.jaroapps.dailybrain.feature.profile.domain.model.UserGender
import com.jaroapps.dailybrain.feature.profile.domain.model.GoogleAccountProfile
import com.jaroapps.dailybrain.feature.profile.domain.model.LinkGoogleAccountResult
import com.jaroapps.dailybrain.feature.profile.domain.model.UserProfile
import com.jaroapps.dailybrain.feature.profile.domain.repository.UserProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.util.UUID
import kotlin.random.Random

class UserProfileRepositoryImpl(
    private val database: AppDatabase,
    private val activeUserProvider: ActiveUserProvider,
) : UserProfileRepository {
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    override fun observeProfile(): Flow<UserProfile> =
        activeUserProvider.observeUserId().flatMapLatest { userId ->
            combine(
                database.userProfileDao().observeProfile(userId),
                database.trainingSessionDao().observeFirstTrainingStartedAtEpochMs(userId),
            ) { entity, firstTrainingAt ->
                val dao = database.userProfileDao()
                if (entity == null) {
                    dao.insertIfAbsent(createInitialProfile(userId, firstTrainingAt))
                    requireNotNull(dao.getProfile(userId)).toDomain()
                } else if (firstTrainingAt != null && firstTrainingAt != entity.createdAtEpochMs) {
                    val updated = entity.copy(createdAtEpochMs = firstTrainingAt)
                    dao.save(updated)
                    updated.toDomain()
                } else {
                    entity.toDomain()
                }
            }
        }

    override fun observeProfiles(): Flow<List<UserProfile>> =
        database.userProfileDao().observeProfiles().map { profiles -> profiles.map { it.toDomain() } }

    override suspend fun createLocalProfile(): UserProfile {
        val localAccountId = UUID.randomUUID().toString()
        val profile = createInitialProfile(
            userId = localAccountId,
            firstTrainingAt = null,
        )
        database.userProfileDao().insertIfAbsent(profile)
        return requireNotNull(database.userProfileDao().getProfile(localAccountId)).toDomain()
    }

    override suspend fun switchActiveProfile(localAccountId: String) {
        requireNotNull(database.userProfileDao().getProfile(localAccountId)) {
            "Local profile does not exist: $localAccountId"
        }
        database.appSettingsDao().setActiveAccount(
            AppSettingsEntity(activeLocalAccountId = localAccountId),
        )
    }

    override suspend fun deleteActiveProfile() {
        database.withTransaction {
            val profileDao = database.userProfileDao()
            val profiles = profileDao.getProfiles()
            val activeId = activeUserProvider.currentUserId()
            val activeProfile = profiles.firstOrNull { it.localAccountId == activeId }
                ?: throw IllegalStateException("Active profile does not exist")
            val replacement = profiles.firstOrNull { it.localAccountId != activeId } ?: run {
                // Keep the invariant that there is always one active local profile.
                // Deleting the last profile therefore replaces it with a fresh empty profile.
                val newProfile = createInitialProfile(UUID.randomUUID().toString(), firstTrainingAt = null)
                profileDao.insertIfAbsent(newProfile)
                requireNotNull(profileDao.getProfile(newProfile.localAccountId))
            }

            database.appSettingsDao().setActiveAccount(
                AppSettingsEntity(activeLocalAccountId = replacement.localAccountId),
            )
            database.trainingSessionDao().deleteAll(activeId)
            database.trainingDayDao().deleteAll(activeId)
            database.dailyExerciseResultDao().deleteAll(activeId)
            database.statisticsDao().deleteOverall(activeId)
            database.statisticsDao().deleteAllExerciseStatistics(activeId)
            database.onboardingProgressDao().deleteProgress(activeId)
            val deleted = profileDao.deleteByLocalAccountId(activeProfile.localAccountId)
            check(deleted == 1) { "Failed to delete local profile" }
        }
    }

    override suspend fun linkGoogleAccount(
        localAccountId: String,
        googleAccount: GoogleAccountProfile,
    ): LinkGoogleAccountResult = database.withTransaction {
        require(googleAccount.subject.isNotBlank()) { "Google account subject must not be blank" }
        val dao = database.userProfileDao()
        val currentProfile = requireNotNull(dao.getProfile(localAccountId)) {
            "Local profile does not exist: $localAccountId"
        }
        val linkedProfile = dao.getProfileByGoogleSubject(googleAccount.subject)
        if (linkedProfile != null && linkedProfile.localAccountId != localAccountId) {
            return@withTransaction LinkGoogleAccountResult.ALREADY_LINKED_TO_ANOTHER_PROFILE
        }

        dao.save(
            currentProfile.copy(
                googleSubject = googleAccount.subject,
                googleEmail = googleAccount.email,
                googleDisplayName = googleAccount.displayName,
                googlePhotoUrl = googleAccount.photoUrl,
                updatedAtEpochMs = System.currentTimeMillis(),
            ),
        )
        LinkGoogleAccountResult.LINKED
    }

    override suspend fun saveProfile(
        displayName: String?,
        gender: String?,
        birthDate: String?,
        country: String?,
        city: String?,
    ) {
        val dao = database.userProfileDao()
        val userId = activeUserProvider.currentUserId()
        val current = dao.getProfile(userId) ?: run {
            dao.insertIfAbsent(createInitialProfile(userId, database.trainingSessionDao().getFirstTrainingStartedAtEpochMs(userId)))
            requireNotNull(dao.getProfile(userId))
        }
        val normalizedBirthDate = birthDate?.trim()?.takeIf(String::isNotEmpty)?.let {
            LocalDate.parse(it).toString()
        }
        dao.save(
            current.copy(
                displayName = displayName?.trim()?.takeIf(String::isNotEmpty),
                gender = gender?.takeIf { value -> UserGender.entries.any { it.name == value } },
                birthDate = normalizedBirthDate,
                country = country?.trim()?.takeIf(String::isNotEmpty),
                city = city?.trim()?.takeIf(String::isNotEmpty),
                updatedAtEpochMs = System.currentTimeMillis(),
            )
        )
    }

    private fun createInitialProfile(userId: String, firstTrainingAt: Long?): UserProfileEntity {
        val now = System.currentTimeMillis()
        return UserProfileEntity(
            localAccountId = userId,
            generatedAlias = "brain#${Random.nextInt(100, 1000)}",
            createdAtEpochMs = firstTrainingAt ?: now,
            updatedAtEpochMs = now,
        )
    }

    private fun UserProfileEntity.toDomain() = UserProfile(
        localAccountId = localAccountId,
        firebaseUid = firebaseUid,
        displayName = displayName,
        generatedAlias = generatedAlias,
        gender = gender?.let { value -> UserGender.entries.firstOrNull { it.name == value } },
        birthDate = birthDate?.let(LocalDate::parse),
        country = country,
        city = city,
        createdAtEpochMs = createdAtEpochMs,
        updatedAtEpochMs = updatedAtEpochMs,
        googleSubject = googleSubject,
        googleEmail = googleEmail,
        googleDisplayName = googleDisplayName,
        googlePhotoUrl = googlePhotoUrl,
    )
}
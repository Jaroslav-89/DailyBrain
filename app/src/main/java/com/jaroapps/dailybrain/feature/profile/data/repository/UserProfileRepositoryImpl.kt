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
            AppSettingsEntity(activeLocalAccountId = localAccountId, profileSelectionRequired = false),
        )
    }

    override suspend fun exitActiveProfile() {
        val activeId = activeUserProvider.currentUserId()
        requireNotNull(database.userProfileDao().getProfile(activeId)) {
            "Active profile does not exist"
        }
        database.appSettingsDao().setProfileSelectionRequired(true)
    }

    override suspend fun deleteActiveProfile() {
        deleteProfile(activeUserProvider.currentUserId())
    }

    override suspend fun deleteProfile(localAccountId: String) {
        database.withTransaction {
            val profileDao = database.userProfileDao()
            val profile = profileDao.getProfile(localAccountId) ?: return@withTransaction
            val settingsDao = database.appSettingsDao()
            val activeId = settingsDao.getActiveLocalAccountId()

            // Deleting the active profile must never make ActiveUserProvider recreate it.
            // Keep the app in profile-selection mode until the user explicitly chooses another one.
            if (activeId == localAccountId) {
                val replacement = profileDao.getProfiles().firstOrNull {
                    it.localAccountId != localAccountId
                }
                if (replacement != null) {
                    settingsDao.setActiveAccount(
                        AppSettingsEntity(
                            activeLocalAccountId = replacement.localAccountId,
                            profileSelectionRequired = true,
                        ),
                    )
                } else {
                    settingsDao.setActiveAccount(
                        AppSettingsEntity(
                            activeLocalAccountId = "",
                            profileSelectionRequired = true,
                        ),
                    )
                }
            }

            database.trainingSessionDao().deleteAll(localAccountId)
            database.trainingDayDao().deleteAll(localAccountId)
            database.dailyExerciseResultDao().deleteAll(localAccountId)
            database.statisticsDao().deleteOverall(localAccountId)
            database.statisticsDao().deleteAllExerciseStatistics(localAccountId)
            database.onboardingProgressDao().deleteProgress(localAccountId)
            val deleted = profileDao.deleteByLocalAccountId(profile.localAccountId)
            check(deleted == 1) { "Failed to delete local profile" }
        }
    }

    override suspend fun isGoogleSubjectLinkedToAnotherProfile(
        localAccountId: String,
        googleSubject: String,
    ): Boolean {
        val linkedProfile = database.userProfileDao().getProfileByGoogleSubject(googleSubject)
        return linkedProfile != null && linkedProfile.localAccountId != localAccountId
    }

    override suspend fun linkGoogleAccount(
        localAccountId: String,
        googleAccount: GoogleAccountProfile,
        firebaseUid: String,
        username: String?,
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
                firebaseUid = firebaseUid,
                username = username?.takeIf(String::isNotBlank),
                googleSubject = googleAccount.subject,
                googleEmail = googleAccount.email,
                googleDisplayName = googleAccount.displayName,
                googlePhotoUrl = googleAccount.photoUrl,
                updatedAtEpochMs = System.currentTimeMillis(),
            ),
        )
        LinkGoogleAccountResult.LINKED
    }

    override suspend fun updateUsername(localAccountId: String, username: String?) {
        database.userProfileDao().updateUsername(
            localAccountId = localAccountId,
            username = username?.takeIf(String::isNotBlank),
            updatedAtEpochMs = System.currentTimeMillis(),
        )
    }

    override suspend fun unlinkGoogleAccount(localAccountId: String) {
        database.withTransaction {
            val dao = database.userProfileDao()
            val profile = requireNotNull(dao.getProfile(localAccountId)) {
                "Local profile does not exist: $localAccountId"
            }
            dao.save(
                profile.copy(
                    firebaseUid = null,
                    username = null,
                    googleSubject = null,
                    googleEmail = null,
                    googleDisplayName = null,
                    googlePhotoUrl = null,
                    updatedAtEpochMs = System.currentTimeMillis(),
                ),
            )
        }
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
        username = username,
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
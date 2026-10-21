package com.jaroapps.dailybrain.feature.profile.data.repository

import com.jaroapps.dailybrain.core.database.AppDatabase
import com.jaroapps.dailybrain.core.database.ActiveUserProvider
import com.jaroapps.dailybrain.core.database.entity.UserProfileEntity
import com.jaroapps.dailybrain.feature.profile.domain.model.UserGender
import com.jaroapps.dailybrain.feature.profile.domain.model.UserProfile
import com.jaroapps.dailybrain.feature.profile.domain.repository.UserProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import java.time.LocalDate
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
    )
}

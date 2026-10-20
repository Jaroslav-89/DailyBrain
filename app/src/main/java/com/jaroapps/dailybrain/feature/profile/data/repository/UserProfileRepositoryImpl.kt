package com.jaroapps.dailybrain.feature.profile.data.repository

import com.jaroapps.dailybrain.core.database.AppDatabase
import com.jaroapps.dailybrain.core.database.entity.UserProfileEntity
import com.jaroapps.dailybrain.feature.profile.domain.model.UserGender
import com.jaroapps.dailybrain.feature.profile.domain.model.UserProfile
import com.jaroapps.dailybrain.feature.profile.domain.repository.UserProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.util.UUID
import kotlin.random.Random

class UserProfileRepositoryImpl(
    private val database: AppDatabase,
) : UserProfileRepository {
    override fun observeProfile(): Flow<UserProfile> =
        database.userProfileDao().observeProfile().map { entity ->
            if (entity != null) {
                entity.toDomain()
            } else {
                val dao = database.userProfileDao()
                dao.insertIfAbsent(createInitialProfile())
                requireNotNull(dao.getProfile()).toDomain()
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
        val current = dao.getProfile() ?: run {
            dao.insertIfAbsent(createInitialProfile())
            requireNotNull(dao.getProfile())
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

    private suspend fun createInitialProfile(): UserProfileEntity {
        val now = System.currentTimeMillis()
        val registeredAt = database.trainingSessionDao().getFirstTrainingStartedAtEpochMs() ?: now
        return UserProfileEntity(
            localAccountId = UUID.randomUUID().toString(),
            generatedAlias = "brain#${Random.nextInt(100, 1000)}",
            createdAtEpochMs = registeredAt,
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

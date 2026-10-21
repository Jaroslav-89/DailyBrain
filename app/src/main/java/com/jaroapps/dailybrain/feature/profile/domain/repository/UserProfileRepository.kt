package com.jaroapps.dailybrain.feature.profile.domain.repository

import com.jaroapps.dailybrain.feature.profile.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface UserProfileRepository {
    fun observeProfile(): Flow<UserProfile>
    fun observeProfiles(): Flow<List<UserProfile>>
    suspend fun createLocalProfile(): UserProfile
    suspend fun switchActiveProfile(localAccountId: String)
    suspend fun deleteActiveProfile()
    suspend fun saveProfile(
        displayName: String?,
        gender: String?,
        birthDate: String?,
        country: String?,
        city: String?,
    )
}
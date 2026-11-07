package com.jaroapps.dailybrain.feature.profile.domain.repository

import com.jaroapps.dailybrain.feature.profile.domain.model.UserProfile
import com.jaroapps.dailybrain.feature.profile.domain.model.GoogleAccountProfile
import com.jaroapps.dailybrain.feature.profile.domain.model.LinkGoogleAccountResult
import kotlinx.coroutines.flow.Flow

interface UserProfileRepository {
    fun observeProfile(): Flow<UserProfile>
    fun observeProfiles(): Flow<List<UserProfile>>
    suspend fun getProfile(localAccountId: String): UserProfile?
    suspend fun createLocalProfile(): UserProfile
    suspend fun switchActiveProfile(localAccountId: String)
    suspend fun exitActiveProfile()
    suspend fun deleteProfile(localAccountId: String)
    suspend fun deleteActiveProfile()
    suspend fun isGoogleSubjectLinkedToAnotherProfile(
        localAccountId: String,
        googleSubject: String,
    ): Boolean
    suspend fun linkGoogleAccount(
        localAccountId: String,
        googleAccount: GoogleAccountProfile,
        firebaseUid: String,
        username: String?,
    ): LinkGoogleAccountResult
    suspend fun updateUsername(localAccountId: String, username: String?)
    suspend fun cacheInviteCode(localAccountId: String, firebaseUid: String, inviteCode: String)
    suspend fun unlinkGoogleAccount(localAccountId: String)
    suspend fun saveProfile(
        displayName: String?,
        gender: String?,
        birthDate: String?,
        country: String?,
        city: String?,
    )
}
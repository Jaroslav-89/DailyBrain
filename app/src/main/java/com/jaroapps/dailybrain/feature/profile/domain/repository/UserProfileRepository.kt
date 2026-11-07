package com.jaroapps.dailybrain.feature.profile.domain.repository

import com.jaroapps.dailybrain.feature.profile.domain.model.UserProfile
import com.jaroapps.dailybrain.feature.profile.domain.model.GoogleAccountProfile
import com.jaroapps.dailybrain.feature.profile.domain.model.LinkGoogleAccountResult
import com.jaroapps.dailybrain.feature.profile.domain.model.PendingProfileDeletion
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface UserProfileRepository {
    fun observeProfile(): Flow<UserProfile>
    /** Emits null when profile selection is active or the selected local row is missing. */
    fun observeSelectedProfile(): Flow<UserProfile?>
    fun observeProfiles(): Flow<List<UserProfile>>
    suspend fun getProfile(localAccountId: String): UserProfile?
    suspend fun createLocalProfile(): UserProfile
    suspend fun switchActiveProfile(localAccountId: String)
    suspend fun exitActiveProfile()
    suspend fun deleteProfile(localAccountId: String): PendingProfileDeletion?
    suspend fun deleteActiveProfile()
    suspend fun findPendingProfileDeletion(googleSubject: String?, firebaseUid: String): PendingProfileDeletion?
    suspend fun markPendingProfileDeletionCloudDataDeleted(firebaseUid: String)
    suspend fun completePendingProfileDeletion(firebaseUid: String)
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
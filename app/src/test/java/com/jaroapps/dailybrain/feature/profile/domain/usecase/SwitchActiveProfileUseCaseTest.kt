package com.jaroapps.dailybrain.feature.profile.domain.usecase

import com.jaroapps.dailybrain.common.domain.service.TrainingStateChangedNotifier
import com.jaroapps.dailybrain.feature.profile.domain.model.GoogleAccountProfile
import com.jaroapps.dailybrain.feature.profile.domain.model.LinkGoogleAccountResult
import com.jaroapps.dailybrain.feature.profile.domain.model.PendingProfileDeletion
import com.jaroapps.dailybrain.feature.profile.domain.model.UserProfile
import com.jaroapps.dailybrain.feature.profile.domain.repository.UserProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SwitchActiveProfileUseCaseTest {

    @Test
    fun switchesLocalProfileWithoutAuthenticationOrNetworkDependencies() = runBlocking {
        val repository = LocalOnlyUserProfileRepository()
        var stateChanged = false
        val useCase = SwitchActiveProfileUseCase(
            repository = repository,
            stateChangedNotifier = TrainingStateChangedNotifier { stateChanged = true },
        )

        useCase("local-profile-b")

        assertEquals("local-profile-b", repository.switchedProfileId)
        assertTrue(stateChanged)
    }

    private class LocalOnlyUserProfileRepository : UserProfileRepository {
        var switchedProfileId: String? = null

        override fun observeProfile(): Flow<UserProfile> = emptyFlow()
        override fun observeSelectedProfile(): Flow<UserProfile?> = emptyFlow()
        override fun observeProfiles(): Flow<List<UserProfile>> = emptyFlow()
        override suspend fun getProfile(localAccountId: String): UserProfile? = null
        override suspend fun createLocalProfile(): UserProfile = error("Not used by the switch operation.")
        override suspend fun switchActiveProfile(localAccountId: String) {
            switchedProfileId = localAccountId
        }
        override suspend fun exitActiveProfile() = Unit
        override suspend fun deleteProfile(localAccountId: String): PendingProfileDeletion? = null
        override suspend fun findPendingProfileDeletion(
            googleSubject: String?,
            firebaseUid: String,
        ): PendingProfileDeletion? = null
        override suspend fun markPendingProfileDeletionCloudDataDeleted(firebaseUid: String) = Unit
        override suspend fun completePendingProfileDeletion(firebaseUid: String) = Unit
        override suspend fun deleteActiveProfile() = Unit
        override suspend fun isGoogleSubjectLinkedToAnotherProfile(
            localAccountId: String,
            googleSubject: String,
        ): Boolean = error("Not used by the switch operation.")
        override suspend fun linkGoogleAccount(
            localAccountId: String,
            googleAccount: GoogleAccountProfile,
            firebaseUid: String,
            username: String?,
        ): LinkGoogleAccountResult = error("Not used by the switch operation.")
        override suspend fun updateUsername(localAccountId: String, username: String?) = Unit
        override suspend fun cacheInviteCode(localAccountId: String, firebaseUid: String, inviteCode: String) = Unit
        override suspend fun unlinkGoogleAccount(localAccountId: String) = Unit
        override suspend fun saveProfile(
            displayName: String?,
            gender: String?,
            birthDate: String?,
            country: String?,
            city: String?,
        ) = Unit
    }
}

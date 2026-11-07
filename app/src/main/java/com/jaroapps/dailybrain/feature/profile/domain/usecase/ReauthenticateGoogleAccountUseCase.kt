package com.jaroapps.dailybrain.feature.profile.domain.usecase

import com.jaroapps.dailybrain.feature.profile.domain.repository.ProfileAuthSessionManager
import com.jaroapps.dailybrain.feature.profile.domain.repository.ProfileGoogleAccountNotLinkedException
import com.jaroapps.dailybrain.feature.profile.domain.repository.UserProfileRepository

/**
 * Restores the Firebase session for an existing profile binding without modifying that binding.
 */
class ReauthenticateGoogleAccountUseCase(
    private val userProfileRepository: UserProfileRepository,
    private val profileAuthSessionManager: ProfileAuthSessionManager,
) {
    suspend operator fun invoke(localAccountId: String) {
        val profile = requireNotNull(userProfileRepository.getProfile(localAccountId)) {
            "Local profile does not exist: $localAccountId"
        }
        if (profile.firebaseUid.isNullOrBlank() || profile.googleSubject.isNullOrBlank()) {
            throw ProfileGoogleAccountNotLinkedException()
        }
        profileAuthSessionManager.reauthenticate(profile)
    }
}

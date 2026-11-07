package com.jaroapps.dailybrain.feature.profile.domain.usecase

import com.jaroapps.dailybrain.feature.profile.domain.repository.UserDirectoryRepository
import com.jaroapps.dailybrain.feature.profile.domain.repository.UserProfileRepository

/** Applies cloud privacy controls first, then persists the same values for the local profile. */
class SaveProfilePrivacySettingsUseCase(
    private val profileRepository: UserProfileRepository,
    private val userDirectoryRepository: UserDirectoryRepository,
) {
    suspend operator fun invoke(
        localAccountId: String,
        profileInfoVisible: Boolean,
        feedUpdatesEnabled: Boolean,
    ) {
        val profile = requireNotNull(profileRepository.getProfile(localAccountId)) {
            "Local profile does not exist."
        }
        profile.firebaseUid?.takeIf(String::isNotBlank)?.let { firebaseUid ->
            userDirectoryRepository.updatePrivacySettings(
                firebaseUid = firebaseUid,
                profileInfoVisible = profileInfoVisible,
                feedUpdatesEnabled = feedUpdatesEnabled,
            )
        }
        profileRepository.savePrivacySettings(
            localAccountId = localAccountId,
            profileInfoVisible = profileInfoVisible,
            feedUpdatesEnabled = feedUpdatesEnabled,
        )
    }
}

package com.jaroapps.dailybrain.feature.profile.domain.repository

import com.jaroapps.dailybrain.feature.profile.domain.model.PublicUserProfile
import com.jaroapps.dailybrain.feature.profile.domain.model.UserPrivacySettings
import com.jaroapps.dailybrain.feature.profile.domain.repository.ProfileStatistics

class UserDirectoryNotConfiguredException : IllegalStateException("Firebase is not configured.")
class FirebaseSessionMismatchException : IllegalStateException("The active Firebase account does not match the selected local profile.")

/**
 * Shared guard used immediately before every Firebase/Firestore operation that belongs to a local profile.
 * A stored Google binding is not enough: FirebaseAuth must hold the matching UID.
 */
fun requireMatchingFirebaseSession(expectedUid: String, currentUid: String?) {
    if (expectedUid.isBlank() || currentUid != expectedUid) {
        throw FirebaseSessionMismatchException()
    }
}

class UsernameTakenException : IllegalStateException("The username is already reserved.")
class UserDirectoryProfileMissingException : IllegalStateException("The cloud user profile does not exist.")
class PublicProfileDeletedException : IllegalStateException("Профиль удалён.")

interface UserDirectoryRepository {
    suspend fun ensureProfile(firebaseUid: String, email: String?, displayName: String?, avatarUrl: String?): String
    suspend fun isUsernameAvailable(firebaseUid: String, username: String): Boolean
    suspend fun updateUsername(firebaseUid: String, username: String)
    suspend fun unlinkProfile(firebaseUid: String)
    suspend fun deleteProfileData(firebaseUid: String)
    suspend fun findUserByUsername(firebaseUid: String, username: String): PublicUserProfile?
    suspend fun getOrCreateInviteCode(firebaseUid: String): String
    suspend fun findUserByInviteCode(firebaseUid: String, inviteCode: String): PublicUserProfile?
    suspend fun followUser(firebaseUid: String, targetFirebaseUid: String)
    suspend fun isFollowing(firebaseUid: String, targetFirebaseUid: String): Boolean
    suspend fun unfollowUser(firebaseUid: String, targetFirebaseUid: String)
    suspend fun getFollowing(firebaseUid: String): List<PublicUserProfile>
    suspend fun getFollowers(firebaseUid: String): List<PublicUserProfile>
    suspend fun getPrivacySettings(firebaseUid: String): UserPrivacySettings?
    suspend fun updatePrivacySettings(firebaseUid: String, profileInfoVisible: Boolean, feedUpdatesEnabled: Boolean)
    suspend fun updatePublicProfile(
        firebaseUid: String,
        displayName: String?,
        country: String?,
        city: String?,
        gender: String?,
        memberSinceEpochMs: Long,
        avatarConfigurationJson: String,
        statistics: ProfileStatistics,
    )
    suspend fun getPublicUserProfile(viewerFirebaseUid: String, profileFirebaseUid: String): PublicUserProfile?
    suspend fun getPublicFollowing(viewerFirebaseUid: String, profileFirebaseUid: String): List<PublicUserProfile>
    suspend fun getPublicFollowers(viewerFirebaseUid: String, profileFirebaseUid: String): List<PublicUserProfile>
    suspend fun blockUser(blockerFirebaseUid: String, blockedFirebaseUid: String)
    suspend fun unblockUser(blockerFirebaseUid: String, blockedFirebaseUid: String)
    suspend fun isBlockedByProfile(profileFirebaseUid: String, viewerFirebaseUid: String): Boolean
    suspend fun isUserBlocked(blockerFirebaseUid: String, blockedFirebaseUid: String): Boolean
    suspend fun getBlockedUsers(blockerFirebaseUid: String): List<PublicUserProfile>
}
